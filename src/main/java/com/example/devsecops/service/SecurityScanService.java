package com.example.devsecops.service;

import com.example.devsecops.model.Scenario;
import com.example.devsecops.model.SecurityScan;
import com.example.devsecops.model.Severity;
import com.example.devsecops.model.Vulnerability;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Supplies representative (recorded) results of JUnit, SonarQube, Gitleaks and OWASP Dependency-Check.
 * The real tools run in GitHub Actions; this service lets the web app demonstrate three fixed scenarios.
 * All findings are fictional demo data - no real credentials or CVEs are used.
 */
@Service
public class SecurityScanService {

    public SecurityScan generate(Scenario scenario) {
        return switch (scenario) {
            case SAFE -> new SecurityScan(48, 0, true, 0, 0, 3, 0, null, 142, 0, 0, 0, 2, List.of(
                    new Vulnerability("Outdated transitive dependency", Severity.LOW, "OWASP", "Open",
                            "Upgrade during the next maintenance window"),
                    new Vulnerability("Unused import / minor code smell", Severity.LOW, "SonarQube", "Open",
                            "Clean up code")));
            case MEDIUM -> new SecurityScan(52, 0, false, 2, 1, 14, 0, null, 156, 0, 0, 1, 3, List.of(
                    new Vulnerability("Vulnerable dependency (medium CVSS)", Severity.MEDIUM, "OWASP", "Open",
                            "Upgrade dependency to the patched version"),
                    new Vulnerability("Unvalidated user input in controller", Severity.MEDIUM, "SonarQube", "Review",
                            "Refactor code: validate and sanitise input"),
                    new Vulnerability("Cognitive complexity too high", Severity.LOW, "SonarQube", "Open",
                            "Split the method into smaller units"),
                    new Vulnerability("Outdated transitive dependency", Severity.LOW, "OWASP", "Open",
                            "Upgrade during the next maintenance window")));
            case CRITICAL -> new SecurityScan(45, 3, false, 4, 3, 21, 1, Severity.CRITICAL, 171, 0, 2, 3, 4, List.of(
                    new Vulnerability("Hardcoded Secret (demo placeholder token in ConfigClient)", Severity.CRITICAL,
                            "Gitleaks", "Open", "Remove secret from source, rotate it, load it from a vault or env var"),
                    new Vulnerability("Vulnerable Dependency (high CVSS)", Severity.HIGH, "OWASP", "Open",
                            "Upgrade dependency to the patched version"),
                    new Vulnerability("Vulnerable Dependency (high CVSS, transitive)", Severity.HIGH, "OWASP", "Open",
                            "Override the transitive version in dependencyManagement"),
                    new Vulnerability("Code Vulnerability: SQL built from user input", Severity.MEDIUM, "SonarQube", "Review",
                            "Use parameterised queries"),
                    new Vulnerability("Failed Security Test: authorization check", Severity.MEDIUM, "JUnit", "Open",
                            "Fix test failure and the underlying access check"),
                    new Vulnerability("Vulnerable Dependency (medium CVSS)", Severity.MEDIUM, "OWASP", "Open",
                            "Upgrade dependency to the patched version")));
            case LIVE -> throw new IllegalArgumentException("LIVE scans are produced by LocalScanService");
        };
    }
}
