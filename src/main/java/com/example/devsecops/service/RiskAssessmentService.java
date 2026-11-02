package com.example.devsecops.service;

import com.example.devsecops.config.RiskProperties;
import com.example.devsecops.model.RiskAssessment;
import com.example.devsecops.model.RiskFactor;
import com.example.devsecops.model.RiskLevel;
import com.example.devsecops.model.SecurityScan;
import com.example.devsecops.model.Severity;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Explainable, rule-based risk engine (NOT a machine-learning model).
 * Every scanner signal adds a fixed number of points; the total maps to LOW / MEDIUM / HIGH.
 */
@Service
public class RiskAssessmentService {

    static final int CRITICAL_SECRET = 50;
    static final int OTHER_SECRET = 20;
    static final int CRITICAL_VULNERABILITY = 50;
    static final int HIGH_VULNERABILITY = 25;
    static final int MEDIUM_VULNERABILITY = 15;
    static final int QUALITY_GATE_FAILURE = 15;
    static final int FAILED_TESTS = 10;
    static final int SKIPPED_SCANNER = 10;

    private final RiskProperties properties;

    public RiskAssessmentService(RiskProperties properties) {
        this.properties = properties;
    }

    public RiskAssessment assess(SecurityScan scan) {
        List<RiskFactor> factors = new ArrayList<>();
        if (scan.secrets() > 0) {
            if (scan.secretSeverity() == Severity.CRITICAL) {
                factors.add(new RiskFactor("Critical secret detected by Gitleaks (" + scan.secrets() + ")", CRITICAL_SECRET));
            } else {
                factors.add(new RiskFactor("Non-critical secret detected by Gitleaks (" + scan.secrets() + ")", OTHER_SECRET));
            }
        }
        if (scan.depCritical() > 0) {
            factors.add(new RiskFactor("Critical dependency vulnerability (" + scan.depCritical() + ")", CRITICAL_VULNERABILITY));
        }
        if (scan.depHigh() > 0) {
            factors.add(new RiskFactor("High dependency vulnerability (" + scan.depHigh() + ")", HIGH_VULNERABILITY));
        }
        if (scan.depMedium() > 0) {
            factors.add(new RiskFactor("Medium dependency vulnerability (" + scan.depMedium() + ")", MEDIUM_VULNERABILITY));
        }
        if (!scan.qualityGatePassed()) {
            factors.add(new RiskFactor("SonarQube quality gate failed", QUALITY_GATE_FAILURE));
        }
        if (scan.testsFailed() > 0) {
            factors.add(new RiskFactor("Failed JUnit tests (" + scan.testsFailed() + ")", FAILED_TESTS));
        }

        if (!scan.skipped().isEmpty()) {
            factors.add(new RiskFactor("Scanner not run, results unverified: " + String.join(", ", scan.skipped().keySet()),
                    SKIPPED_SCANNER));
        }

        int score = Math.min(100, factors.stream().mapToInt(RiskFactor::points).sum());
        RiskLevel level = score >= properties.highThreshold() ? RiskLevel.HIGH
                : score >= properties.mediumThreshold() ? RiskLevel.MEDIUM : RiskLevel.LOW;
        return new RiskAssessment(score, level, List.copyOf(factors), recommendedAction(level));
    }

    /** The scoring table, shown on the Risk Analysis and Settings pages. */
    public List<RiskFactor> rules() {
        return List.of(
                new RiskFactor("Critical secret (Gitleaks)", CRITICAL_SECRET),
                new RiskFactor("Non-critical secret (Gitleaks)", OTHER_SECRET),
                new RiskFactor("Critical dependency vulnerability (OWASP)", CRITICAL_VULNERABILITY),
                new RiskFactor("High dependency vulnerability (OWASP)", HIGH_VULNERABILITY),
                new RiskFactor("Medium dependency vulnerability (OWASP)", MEDIUM_VULNERABILITY),
                new RiskFactor("SonarQube quality gate failure", QUALITY_GATE_FAILURE),
                new RiskFactor("Failed JUnit tests", FAILED_TESTS),
                new RiskFactor("Scanner skipped (live scans only)", SKIPPED_SCANNER));
    }

    private String recommendedAction(RiskLevel level) {
        return switch (level) {
            case LOW -> "PROCEED WITH DEPLOYMENT";
            case MEDIUM -> "REQUEST SECURITY REVIEW BEFORE DEPLOYMENT";
            case HIGH -> "BLOCK DEPLOYMENT";
        };
    }
}
