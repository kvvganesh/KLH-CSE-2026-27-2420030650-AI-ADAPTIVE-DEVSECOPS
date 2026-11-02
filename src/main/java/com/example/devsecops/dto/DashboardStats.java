package com.example.devsecops.dto;

public record DashboardStats(int totalProjects, int pipelineRuns, int securityIssues,
                             int criticalVulnerabilities, int secretsDetected, int blockedDeployments) {
}
