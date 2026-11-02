package com.example.devsecops.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.devsecops.config.RiskProperties;
import com.example.devsecops.model.RiskAssessment;
import com.example.devsecops.model.SecurityScan;
import com.example.devsecops.model.Vulnerability;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalScanServiceTest {

    @TempDir
    Path tmp;

    @Test
    void parsesSurefireReports() throws Exception {
        Path dir = Files.createDirectories(tmp.resolve("target/surefire-reports"));
        Files.writeString(dir.resolve("TEST-a.xml"),
                "<testsuite name=\"a\"><testcase classname=\"A\" name=\"ok1\"/><testcase classname=\"A\" name=\"ok2\"/>"
                        + "<testcase classname=\"A\" name=\"bad\"><failure message=\"x\"/></testcase></testsuite>");
        List<Vulnerability> findings = new ArrayList<>();
        LocalScanService.Junit result = LocalScanService.parseSurefire(tmp, findings);
        assertEquals(3, result.total());
        assertEquals(1, result.failed());
        assertEquals(1, findings.size());
    }

    @Test
    void noReportsMeansNoTests() {
        LocalScanService.Junit result = LocalScanService.parseSurefire(tmp, new ArrayList<>());
        assertEquals(0, result.total());
        assertEquals(0, result.failed());
    }

    @Test
    void skippedScannerAddsRisk() {
        SecurityScan scan = new SecurityScan(5, 0, true, 0, 0, 0, 0, null, 0, 0, 0, 0, 0, List.of(),
                Map.of("GITLEAKS", "not installed"));
        RiskAssessment risk = new RiskAssessmentService(new RiskProperties(30, 60)).assess(scan);
        assertEquals(10, risk.score());
        assertTrue(risk.factors().get(0).description().contains("GITLEAKS"));
    }
}
