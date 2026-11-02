package com.example.devsecops.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.devsecops.config.RiskProperties;
import com.example.devsecops.dto.DeploymentResult;
import com.example.devsecops.model.Decision;
import com.example.devsecops.model.PipelineRun;
import com.example.devsecops.model.Project;
import com.example.devsecops.model.RiskAssessment;
import com.example.devsecops.model.RiskLevel;
import com.example.devsecops.model.Scenario;
import com.example.devsecops.repository.PipelineRunRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class DeploymentServiceTest {

    private final DeploymentService service = new DeploymentService();

    private RiskAssessment risk(RiskLevel level) {
        return new RiskAssessment(0, level, List.of(), "x");
    }

    @Test
    void lowRiskIsApproved() {
        assertEquals(Decision.APPROVED, service.decide(risk(RiskLevel.LOW)).decision());
    }

    @Test
    void mediumRiskRequiresReview() {
        assertEquals(Decision.REVIEW_REQUIRED, service.decide(risk(RiskLevel.MEDIUM)).decision());
    }

    @Test
    void highRiskIsBlocked() {
        assertEquals(Decision.BLOCKED, service.decide(risk(RiskLevel.HIGH)).decision());
    }

    @Test
    void simulationFollowsTheDecision() {
        PipelineService pipelines = new PipelineService(new PipelineRunRepository(), new SecurityScanService(),
                new RiskAssessmentService(new RiskProperties(30, 60)), service);
        Project project = new Project("Demo", "https://example.com/repo", "main", "", Scenario.CRITICAL);
        project.setId(1L);
        PipelineRun blocked = pipelines.run(project);
        DeploymentResult result = service.simulate(blocked);
        assertEquals(Decision.BLOCKED, result.outcome());
        assertTrue(result.simulated());

        project.setScenario(Scenario.SAFE);
        assertEquals(Decision.APPROVED, service.simulate(pipelines.run(project)).outcome());
    }
}
