package com.example.devsecops.service;

import com.example.devsecops.config.ScanProperties;
import com.example.devsecops.exception.ConflictException;
import com.example.devsecops.model.Project;
import com.example.devsecops.model.SecurityScan;
import com.example.devsecops.model.Severity;
import com.example.devsecops.model.Vulnerability;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import tools.jackson.databind.json.JsonMapper;

/**
 * Real scan of a project: clone, run Maven tests (JUnit), Gitleaks, and optionally SonarQube and
 * OWASP Dependency-Check, then convert the tool output into a {@link SecurityScan}.
 * WARNING: the project's own tests are executed on this machine. Only scan repositories you trust.
 */
@Service
public class LocalScanService {

    private static final Logger log = LoggerFactory.getLogger(LocalScanService.class);
    private static final String DEP_CHECK = "org.owasp:dependency-check-maven:12.1.0:check";
    private static final String SONAR = "org.sonarsource.scanner.maven:sonar-maven-plugin:5.0.0.4389:sonar";

    record Junit(int total, int failed) {
    }

    record Sonar(boolean passed, int bugs, int vulnerabilities, int smells) {
    }

    record Deps(int scanned, int critical, int high, int medium, int low) {
    }

    private final ScanProperties props;
    private final CommandRunner runner = new CommandRunner();
    private final JsonMapper json = JsonMapper.builder().build();
    private final Map<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

    public LocalScanService(ScanProperties props) {
        this.props = props;
    }

    public SecurityScan scan(Project project) {
        ReentrantLock lock = locks.computeIfAbsent(project.getId(), id -> new ReentrantLock());
        if (!lock.tryLock()) {
            throw new ConflictException("A scan is already running for this project. Please wait for it to finish.");
        }
        try {
            return doScan(project);
        } finally {
            lock.unlock();
        }
    }

    private SecurityScan doScan(Project project) {
        Path base = workspace().resolve(String.valueOf(project.getId()));
        Path repo = base.resolve("repo");
        Path logs = base.resolve("logs");
        try {
            Files.createDirectories(logs);
            deleteRecursively(repo);
        } catch (IOException e) {
            throw new ConflictException("Cannot prepare the scan workspace " + base + ": " + e.getMessage());
        }
        clone(project, base, repo, logs);
        if (!Files.exists(repo.resolve("pom.xml"))) {
            throw new ConflictException("No pom.xml in the repository root. Live scanning supports Maven projects only.");
        }

        List<Vulnerability> findings = new ArrayList<>();
        Map<String, String> skipped = new LinkedHashMap<>();

        Junit junit = scanTests(repo, logs, findings);
        int secrets = scanSecrets(repo, base, logs, findings, skipped);
        Sonar sonar = scanSonar(project, repo, logs, findings, skipped);
        Deps deps = scanDependencies(repo, logs, findings, skipped);

        findings.sort(Comparator.comparing(Vulnerability::severity));
        return new SecurityScan(junit.total(), junit.failed(), sonar.passed(), sonar.bugs(), sonar.vulnerabilities(),
                sonar.smells(), secrets, secrets > 0 ? Severity.CRITICAL : null, deps.scanned(), deps.critical(),
                deps.high(), deps.medium(), deps.low(), List.copyOf(findings), Map.copyOf(skipped));
    }

    // ---------- clone ----------

    private void clone(Project project, Path base, Path repo, Path logs) {
        CommandRunner.Result r = runner.run(
                List.of("git", "clone", "--branch", project.getBranch(), "--", project.getRepositoryUrl(), repo.toString()),
                base, Map.of("GIT_TERMINAL_PROMPT", "0"), props.timeoutMinutes(), logs.resolve("git.log"));
        if (r.notFound()) {
            throw new ConflictException("Git was not found on PATH. Install Git to use live scans.");
        }
        if (r.timedOut() || r.exit() != 0) {
            throw new ConflictException("Could not clone " + project.getRepositoryUrl() + " (branch " + project.getBranch()
                    + "). Check the URL, branch and access. Details: " + logs.resolve("git.log"));
        }
    }

    // ---------- JUnit ----------

    private Junit scanTests(Path repo, Path logs, List<Vulnerability> findings) {
        CommandRunner.Result r = runner.run(List.of(maven(), "-B", "test"), repo, Map.of(),
                props.timeoutMinutes(), logs.resolve("junit.log"));
        if (r.notFound()) {
            throw new ConflictException("Maven was not found (" + maven()
                    + "). Install Maven or set devsecops.scan.maven-command.");
        }
        Junit parsed = parseSurefire(repo, findings);
        int total = parsed.total();
        int failed = parsed.failed();
        if (r.timedOut()) {
            failed = Math.max(failed, 1);
            total = Math.max(total, failed);
            findings.add(new Vulnerability("Maven build timed out", Severity.HIGH, "JUnit", "Open",
                    "Check junit.log in the workspace logs folder"));
        } else if (r.exit() != 0 && failed == 0) {
            failed = 1;
            total += 1;
            findings.add(new Vulnerability("Build or test run failed", Severity.HIGH, "JUnit", "Open",
                    "Fix the build. See junit.log in the workspace logs folder"));
        } else if (total == 0) {
            findings.add(new Vulnerability("No automated tests found", Severity.LOW, "JUnit", "Open",
                    "Add unit tests for the application"));
        }
        return new Junit(total, failed);
    }

    static Junit parseSurefire(Path repo, List<Vulnerability> findings) {
        List<Path> files;
        try (Stream<Path> walk = Files.walk(repo)) {
            files = walk.filter(p -> {
                String name = p.getFileName().toString();
                Path parent = p.getParent();
                return name.startsWith("TEST-") && name.endsWith(".xml")
                        && parent != null && "surefire-reports".equals(parent.getFileName().toString());
            }).toList();
        } catch (IOException e) {
            return new Junit(0, 0);
        }
        int total = 0;
        int failed = 0;
        for (Path file : files) {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                Document doc = factory.newDocumentBuilder().parse(file.toFile());
                NodeList cases = doc.getElementsByTagName("testcase");
                for (int i = 0; i < cases.getLength(); i++) {
                    Element testCase = (Element) cases.item(i);
                    total++;
                    boolean bad = testCase.getElementsByTagName("failure").getLength() > 0
                            || testCase.getElementsByTagName("error").getLength() > 0;
                    if (bad) {
                        failed++;
                        if (failed <= 10) {
                            findings.add(new Vulnerability("Failed test: " + testCase.getAttribute("classname") + "."
                                    + testCase.getAttribute("name"), Severity.MEDIUM, "JUnit", "Open", "Fix the failing test"));
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Could not parse test report {}: {}", file, e.getMessage());
            }
        }
        return new Junit(total, failed);
    }

    // ---------- Gitleaks ----------

    private int scanSecrets(Path repo, Path base, Path logs, List<Vulnerability> findings, Map<String, String> skipped) {
        Path report = logs.resolve("gitleaks-report.json");
        try {
            Files.deleteIfExists(report);
        } catch (IOException ignored) {
            // a stale report would be overwritten anyway
        }
        CommandRunner.Result r = runner.run(List.of(props.gitleaksCommand(), "detect", "--source", repo.toString(),
                "--report-format", "json", "--report-path", report.toString(), "--redact", "--no-banner", "--exit-code", "0"),
                base, Map.of(), props.timeoutMinutes(), logs.resolve("gitleaks.log"));
        if (r.notFound()) {
            skipped.put("GITLEAKS", "gitleaks is not installed or not on PATH");
            return 0;
        }
        if (r.timedOut() || r.exit() != 0) {
            skipped.put("GITLEAKS", "gitleaks failed (see gitleaks.log)");
            return 0;
        }
        int count = 0;
        for (Object entry : readList(report)) {
            Map<?, ?> leak = asMap(entry);
            count++;
            if (count <= 20) { // never copy the secret value itself
                findings.add(new Vulnerability("Hardcoded secret (" + str(leak, "RuleID") + ") in " + str(leak, "File")
                        + ":" + str(leak, "StartLine"), Severity.CRITICAL, "Gitleaks", "Open",
                        "Remove the secret from source and history, rotate it, then load it from an environment variable or vault"));
            }
        }
        return count;
    }

    // ---------- SonarQube ----------

    private Sonar scanSonar(Project project, Path repo, Path logs, List<Vulnerability> findings, Map<String, String> skipped) {
        Sonar none = new Sonar(true, 0, 0, 0);
        if (props.sonarUrl().isBlank() || props.sonarToken().isBlank()) {
            skipped.put("SONARQUBE", "not configured (set SONAR_HOST_URL and SONAR_TOKEN)");
            return none;
        }
        String key = "devsecops-" + project.getId() + "-" + project.getName().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        CommandRunner.Result r = runner.run(List.of(maven(), "-B", "-DskipTests", "compile", SONAR,
                "-Dsonar.projectKey=" + key, "-Dsonar.qualitygate.wait=true"), repo,
                Map.of("SONAR_TOKEN", props.sonarToken(), "SONAR_HOST_URL", props.sonarUrl()),
                props.timeoutMinutes(), logs.resolve("sonarqube.log"));
        if (r.notFound() || r.timedOut()) {
            skipped.put("SONARQUBE", "analysis could not run (see sonarqube.log)");
            return none;
        }
        String base = props.sonarUrl().replaceAll("/+$", "");
        Map<?, ?> status = httpGet(base + "/api/qualitygates/project_status?projectKey=" + key);
        String gate = status == null ? "" : str(asMap(status.get("projectStatus")), "status");
        if (!"OK".equals(gate) && !"ERROR".equals(gate)) {
            skipped.put("SONARQUBE", "no quality gate result (analysis failed or server unreachable, see sonarqube.log)");
            return none;
        }
        int bugs = 0;
        int vulns = 0;
        int smells = 0;
        Map<?, ?> measures = httpGet(base + "/api/measures/component?component=" + key
                + "&metricKeys=bugs,vulnerabilities,code_smells");
        if (measures != null) {
            for (Object m : asList(asMap(measures.get("component")).get("measures"))) {
                Map<?, ?> measure = asMap(m);
                int value = toInt(str(measure, "value"));
                switch (str(measure, "metric")) {
                    case "bugs" -> bugs = value;
                    case "vulnerabilities" -> vulns = value;
                    case "code_smells" -> smells = value;
                    default -> { }
                }
            }
        }
        boolean passed = "OK".equals(gate);
        if (!passed) {
            findings.add(new Vulnerability("SonarQube quality gate failed", Severity.MEDIUM, "SonarQube", "Review",
                    "Open the project in SonarQube and fix the failing conditions"));
        }
        if (vulns > 0) {
            findings.add(new Vulnerability(vulns + " code vulnerabilities reported by SonarQube", Severity.MEDIUM,
                    "SonarQube", "Review", "Refactor the flagged code"));
        }
        return new Sonar(passed, bugs, vulns, smells);
    }

    private Map<?, ?> httpGet(String url) {
        try {
            String auth = Base64.getEncoder().encodeToString((props.sonarToken() + ":").getBytes(StandardCharsets.UTF_8));
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Basic " + auth).GET().build();
            HttpResponse<String> response = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return null;
            }
            return asMap(json.readValue(response.body(), Object.class));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            log.warn("SonarQube request failed: {}", e.getMessage());
            return null;
        }
    }

    // ---------- OWASP Dependency-Check ----------

    private Deps scanDependencies(Path repo, Path logs, List<Vulnerability> findings, Map<String, String> skipped) {
        Deps none = new Deps(0, 0, 0, 0, 0);
        if (!props.dependencyCheckEnabled()) {
            skipped.put("OWASP", "disabled (set devsecops.scan.dependency-check-enabled=true; first run downloads the NVD database)");
            return none;
        }
        List<String> command = new ArrayList<>(List.of(maven(), "-B", DEP_CHECK, "-Dformat=JSON", "-DfailBuildOnCVSS=11"));
        if (!props.nvdApiKey().isBlank()) {
            command.add("-DnvdApiKey=" + props.nvdApiKey());
        }
        CommandRunner.Result r = runner.run(command, repo, Map.of(), props.dependencyCheckTimeoutMinutes(),
                logs.resolve("dependency-check.log"));
        List<Path> reports;
        try (Stream<Path> walk = Files.walk(repo)) {
            reports = walk.filter(p -> "dependency-check-report.json".equals(p.getFileName().toString())).toList();
        } catch (IOException e) {
            reports = List.of();
        }
        if (reports.isEmpty()) {
            skipped.put("OWASP", r.timedOut() ? "timed out (see dependency-check.log)" : "failed (see dependency-check.log)");
            return none;
        }
        int scanned = 0;
        int critical = 0;
        int high = 0;
        int medium = 0;
        int low = 0;
        List<Vulnerability> found = new ArrayList<>();
        for (Path report : reports) {
            Map<?, ?> root = asMap(readValue(report));
            List<?> dependencies = asList(root.get("dependencies"));
            scanned += dependencies.size();
            for (Object d : dependencies) {
                Map<?, ?> dependency = asMap(d);
                List<?> vulns = asList(dependency.get("vulnerabilities"));
                Severity worst = null;
                for (Object v : vulns) {
                    Severity severity = severity(str(asMap(v), "severity"));
                    switch (severity) {
                        case CRITICAL -> critical++;
                        case HIGH -> high++;
                        case MEDIUM -> medium++;
                        case LOW -> low++;
                    }
                    if (worst == null || severity.ordinal() < worst.ordinal()) {
                        worst = severity;
                    }
                }
                if (worst != null) {
                    found.add(new Vulnerability("Vulnerable dependency: " + str(dependency, "fileName") + " ("
                            + vulns.size() + " known vulnerabilities, e.g. " + str(asMap(vulns.get(0)), "name") + ")",
                            worst, "OWASP", "Open", "Upgrade to a patched version"));
                }
            }
        }
        found.sort(Comparator.comparing(Vulnerability::severity));
        findings.addAll(found.stream().limit(15).toList());
        return new Deps(scanned, critical, high, medium, low);
    }

    // ---------- helpers ----------

    private static Severity severity(String text) {
        return switch (text.toUpperCase(Locale.ROOT)) {
            case "CRITICAL" -> Severity.CRITICAL;
            case "HIGH" -> Severity.HIGH;
            case "MEDIUM", "MODERATE" -> Severity.MEDIUM;
            default -> Severity.LOW;
        };
    }

    private String maven() {
        if (!props.mavenCommand().isBlank()) {
            return props.mavenCommand();
        }
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win") ? "mvn.cmd" : "mvn";
    }

    private Path workspace() {
        if (!props.workspace().isBlank()) {
            return Path.of(props.workspace());
        }
        return Path.of(System.getProperty("user.home"), ".adaptive-devsecops", "workspaces");
    }

    private Object readValue(Path file) {
        try {
            if (Files.size(file) == 0) {
                return null;
            }
            return json.readValue(file.toFile(), Object.class);
        } catch (Exception e) {
            log.warn("Could not read {}: {}", file, e.getMessage());
            return null;
        }
    }

    private List<?> readList(Path file) {
        return Files.exists(file) ? asList(readValue(file)) : List.of();
    }

    private static List<?> asList(Object o) {
        return o instanceof List<?> list ? list : List.of();
    }

    private static Map<?, ?> asMap(Object o) {
        return o instanceof Map<?, ?> map ? map : Map.of();
    }

    private static String str(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static int toInt(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Deletes a folder, clearing read-only flags first (git objects are read-only on Windows). */
    private static void deleteRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                file.toFile().setWritable(true);
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                Files.delete(d);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
