package com.example.devsecops.model;

import java.util.List;
import java.util.Map;

/**
 * Combined result of the four scanners for one pipeline run.
 * {@code skipped} maps a tool key (SONARQUBE, GITLEAKS, OWASP) to the reason it did not run.
 */
public record SecurityScan(
        int testsTotal, int testsFailed,
        boolean qualityGatePassed, int bugs, int vulnerabilities, int codeSmells,
        int secrets, Severity secretSeverity,
        int dependenciesScanned, int depCritical, int depHigh, int depMedium, int depLow,
        List<Vulnerability> findings,
        Map<String, String> skipped) {

    /** Convenience constructor for results where every tool ran. */
    public SecurityScan(int testsTotal, int testsFailed, boolean qualityGatePassed, int bugs, int vulnerabilities,
                        int codeSmells, int secrets, Severity secretSeverity, int dependenciesScanned,
                        int depCritical, int depHigh, int depMedium, int depLow, List<Vulnerability> findings) {
        this(testsTotal, testsFailed, qualityGatePassed, bugs, vulnerabilities, codeSmells, secrets, secretSeverity,
                dependenciesScanned, depCritical, depHigh, depMedium, depLow, findings, Map.of());
    }

    public int testsPassed() { return testsTotal - testsFailed; }
}
