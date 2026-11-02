package com.example.devsecops.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.devsecops.config.RiskProperties;
import com.example.devsecops.exception.ConflictException;
import com.example.devsecops.model.Decision;
import com.example.devsecops.model.PipelineRun;
import com.example.devsecops.model.Project;
import com.example.devsecops.model.RiskLevel;
import com.example.devsecops.model.RunStatus;
import com.example.devsecops.model.Scenario;
import com.example.devsecops.model.StageStatus;
import com.example.devsecops.repository.PipelineRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PipelineServiceTest {

    private PipelineService pipelines;

    @BeforeEach
    void setUp() {
        pipelines = new PipelineService(new PipelineRunRepository(), new SecurityScanService(),
                new RiskAssessmentService(new RiskProperties(30, 60)), new DeploymentService());
    }

    private Project project(Scenario scenario) {
        Project p = new Project("Demo", "https://example.com/repo", "main", "", scenario);
        p.setId(1L);
        return p;
    }

    @Test
    void safeProjectPassesAllStages() {
        PipelineRun run = pipelines.run(project(Scenario.SAFE));
        assertEquals(RunStatus.PASSED, run.status());
        assertEquals(8, run.stages().size());
        assertEquals(RiskLevel.LOW, run.risk().level());
        assertEquals(StageStatus.PASSED, run.stages().get(7).status());
    }

    @Test
    void mediumProjectWarnsAndWaitsForReview() {
        PipelineRun run = pipelines.run(project(Scenario.MEDIUM));
        assertEquals(RunStatus.WARNING, run.status());
        assertEquals(Decision.REVIEW_REQUIRED, run.decision().decision());
        assertEquals(StageStatus.PENDING, run.stages().get(7).status());
    }

    @Test
    void criticalProjectFailsAndIsBlocked() {
        PipelineRun run = pipelines.run(project(Scenario.CRITICAL));
        assertEquals(RunStatus.FAILED, run.status());
        assertEquals(StageStatus.FAILED, run.stages().get(4).status());
        assertEquals(StageStatus.BLOCKED, run.stages().get(7).status());
        assertEquals(Decision.BLOCKED, run.decision().decision());
    }

    @Test
    void statsCountBlockedRuns() {
        pipelines.run(project(Scenario.CRITICAL));
        pipelines.run(project(Scenario.SAFE));
        assertEquals(2, pipelines.stats(1).pipelineRuns());
        assertEquals(1, pipelines.stats(1).blockedDeployments());
    }

    @Test
    void latestRunRequiredBeforeUse() {
        assertThrows(ConflictException.class, () -> pipelines.requireLatest(99L));
    }
}
