package com.example.devsecops.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.devsecops.config.RiskProperties;
import com.example.devsecops.model.RiskAssessment;
import com.example.devsecops.model.RiskLevel;
import com.example.devsecops.model.Scenario;
import com.example.devsecops.model.SecurityScan;
import com.example.devsecops.model.Severity;
import java.util.List;
import org.junit.jupiter.api.Test;

class RiskAssessmentServiceTest {

    private final RiskAssessmentService service = new RiskAssessmentService(new RiskProperties(30, 60));
    private final SecurityScanService scans = new SecurityScanService();

    @Test
    void criticalVulnerabilityAndSecretIsHigh() {
        SecurityScan scan = new SecurityScan(10, 0, true, 0, 0, 0, 1, Severity.CRITICAL, 50, 1, 0, 0, 0, List.of());
        RiskAssessment risk = service.assess(scan);
        assertEquals(RiskLevel.HIGH, risk.level());
        assertEquals(100, risk.score());
        assertEquals("BLOCK DEPLOYMENT", risk.recommendedAction());
    }

    @Test
    void safeScenarioIsLowWithNoReasons() {
        RiskAssessment risk = service.assess(scans.generate(Scenario.SAFE));
        assertEquals(RiskLevel.LOW, risk.level());
        assertEquals(0, risk.score());
        assertTrue(risk.factors().isEmpty());
    }

    @Test
    void mediumScenarioIsMedium() {
        RiskAssessment risk = service.assess(scans.generate(Scenario.MEDIUM));
        assertEquals(RiskLevel.MEDIUM, risk.level());
        assertEquals(30, risk.score());
    }

    @Test
    void criticalScenarioIsHighAndExplained() {
        RiskAssessment risk = service.assess(scans.generate(Scenario.CRITICAL));
        assertEquals(RiskLevel.HIGH, risk.level());
        assertTrue(risk.factors().stream().anyMatch(f -> f.description().contains("Critical secret")));
    }

    @Test
    void failedTestsAddTenPointsAndStayLow() {
        SecurityScan scan = new SecurityScan(10, 2, true, 0, 0, 0, 0, null, 50, 0, 0, 0, 0, List.of());
        RiskAssessment risk = service.assess(scan);
        assertEquals(10, risk.score());
        assertEquals(RiskLevel.LOW, risk.level());
    }

    @Test
    void highVulnerabilityPlusQualityFailureReachesMedium() {
        SecurityScan scan = new SecurityScan(10, 0, false, 0, 0, 0, 0, null, 50, 0, 1, 0, 0, List.of());
        RiskAssessment risk = service.assess(scan);
        assertEquals(40, risk.score());
        assertEquals(RiskLevel.MEDIUM, risk.level());
    }
}
