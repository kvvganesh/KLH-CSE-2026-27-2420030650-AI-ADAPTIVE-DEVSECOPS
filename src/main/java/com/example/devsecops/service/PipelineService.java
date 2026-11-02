package com.example.devsecops.service;

import com.example.devsecops.dto.DashboardStats;
import com.example.devsecops.exception.ConflictException;
import com.example.devsecops.exception.NotFoundException;
import com.example.devsecops.model.Decision;
import com.example.devsecops.model.DeploymentDecision;
import com.example.devsecops.model.PipelineRun;
import com.example.devsecops.model.Project;
import com.example.devsecops.model.RiskAssessment;
import com.example.devsecops.model.RunStatus;
import com.example.devsecops.model.SecurityScan;
import com.example.devsecops.model.Severity;
import com.example.devsecops.model.Stage;
import com.example.devsecops.model.StageStatus;
import com.example.devsecops.repository.PipelineRunRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.devsecops.model.Scenario;

/** Simulated pipeline: scan -> risk analysis -> deployment decision. The real CI is GitHub Actions. */
@Service
public class PipelineService {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PipelineRunRepository runs;
    private final SecurityScanService scanService;
    private final RiskAssessmentService riskService;
    private final DeploymentService deploymentService;
    private final LocalScanService localScans;

    @Autowired
    public PipelineService(PipelineRunRepository runs, SecurityScanService scanService,
                           RiskAssessmentService riskService, DeploymentService deploymentService,
                           LocalScanService localScans) {
        this.runs = runs;
        this.scanService = scanService;
        this.riskService = riskService;
        this.deploymentService = deploymentService;
        this.localScans = localScans;
    }

    /** Simulated-only pipeline (no live scanning); used by unit tests. */
    public PipelineService(PipelineRunRepository runs, SecurityScanService scanService,
                           RiskAssessmentService riskService, DeploymentService deploymentService) {
        this(runs, scanService, riskService, deploymentService, null);
    }

    private SecurityScan scan(Project project) {
        if (project.getScenario() == Scenario.LIVE) {
            if (localScans == null) {
                throw new ConflictException("Live scanning is not available in this configuration.");
            }
            return localScans.scan(project);
        }
        return scanService.generate(project.getScenario());
    }

    private StageStatus status(SecurityScan scan, String tool, boolean failed) {
        return scan.skipped().containsKey(tool) ? StageStatus.PENDING : failed ? StageStatus.FAILED : StageStatus.PASSED;
    }

    private String describe(SecurityScan scan, String tool, String text) {
        return scan.skipped().containsKey(tool) ? "Skipped: " + scan.skipped().get(tool) : text;
    }

    public PipelineRun run(Project project) {
        SecurityScan scan = scan(project);
        RiskAssessment risk = riskService.assess(scan);
        DeploymentDecision decision = deploymentService.decide(risk);

        StageStatus deployStage = switch (decision.decision()) {
            case APPROVED -> StageStatus.PASSED;
            case REVIEW_REQUIRED -> StageStatus.PENDING;
            case BLOCKED -> StageStatus.BLOCKED;
        };
        List<Stage> stages = List.of(
                new Stage("SOURCE", "Git checkout", StageStatus.PASSED),
                new Stage("BUILD", "Maven compile and package", StageStatus.PASSED),
                new Stage("JUNIT", "Unit and security tests", scan.testsFailed() > 0 ? StageStatus.FAILED : StageStatus.PASSED),
                new Stage("SONARQUBE", describe(scan, "SONARQUBE", "Static code analysis"),
                        status(scan, "SONARQUBE", !scan.qualityGatePassed())),
                new Stage("GITLEAKS", describe(scan, "GITLEAKS", "Secret detection"),
                        status(scan, "GITLEAKS", scan.secrets() > 0)),
                new Stage("OWASP", describe(scan, "OWASP", "Dependency-Check scan"),
                        status(scan, "OWASP", scan.depCritical() + scan.depHigh() > 0)),
                new Stage("RISK ANALYSIS", "Explainable rule-based engine", StageStatus.PASSED),
                new Stage("DEPLOYMENT", "Adaptive decision (simulated)", deployStage));

        RunStatus status = switch (risk.level()) {
            case LOW -> RunStatus.PASSED;
            case MEDIUM -> RunStatus.WARNING;
            case HIGH -> RunStatus.FAILED;
        };
        PipelineRun run = new PipelineRun(runs.nextId(), project.getId(), project.getName(),
                LocalDateTime.now().format(TIME), status, stages, scan, risk, decision);
        return runs.save(run);
    }

    public List<Stage> pendingStages() {
        return List.of("SOURCE", "BUILD", "JUNIT", "SONARQUBE", "GITLEAKS", "OWASP", "RISK ANALYSIS", "DEPLOYMENT").stream()
                .map(n -> new Stage(n, "", StageStatus.PENDING)).toList();
    }

    public Optional<PipelineRun> latest(Long projectId) {
        return runs.findLatest(projectId);
    }

    public PipelineRun requireLatest(Long projectId) {
        return latest(projectId).orElseThrow(() ->
                new ConflictException("This project has no pipeline run yet. Run the pipeline first."));
    }

    public PipelineRun get(long runId) {
        return runs.findById(runId).orElseThrow(() -> new NotFoundException("Pipeline run " + runId + " was not found."));
    }

    public List<PipelineRun> history(Long projectId) {
        return runs.findByProject(projectId);
    }

    public List<PipelineRun> all() {
        return runs.findAll();
    }

    public List<PipelineRun> recent(int limit) {
        return runs.findAll().stream().limit(limit).toList();
    }

    public DashboardStats stats(int totalProjects) {
        int issues = 0;
        int critical = 0;
        int secrets = 0;
        for (PipelineRun r : runs.findLatestPerProject()) {
            issues += r.scan().findings().size();
            critical += (int) r.scan().findings().stream().filter(v -> v.severity() == Severity.CRITICAL).count();
            secrets += r.scan().secrets();
        }
        List<PipelineRun> all = runs.findAll();
        int blocked = (int) all.stream().filter(r -> r.decision().decision() == Decision.BLOCKED).count();
        return new DashboardStats(totalProjects, all.size(), issues, critical, secrets, blocked);
    }
}
