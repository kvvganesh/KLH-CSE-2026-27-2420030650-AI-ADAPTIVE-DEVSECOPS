package com.example.devsecops.model;

import java.util.List;

public record PipelineRun(Long id, Long projectId, String projectName, String startedAt, RunStatus status,
                          List<Stage> stages, SecurityScan scan, RiskAssessment risk, DeploymentDecision decision) {
}
