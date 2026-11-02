package com.example.devsecops.service;

import com.example.devsecops.dto.DeploymentResult;
import com.example.devsecops.model.Decision;
import com.example.devsecops.model.DeploymentDecision;
import com.example.devsecops.model.PipelineRun;
import com.example.devsecops.model.RiskAssessment;
import java.util.List;
import org.springframework.stereotype.Service;

/** Maps risk to a deployment decision and simulates the outcome. Nothing is ever deployed. */
@Service
public class DeploymentService {

    public DeploymentDecision decide(RiskAssessment risk) {
        return switch (risk.level()) {
            case LOW -> new DeploymentDecision(Decision.APPROVED,
                    "Risk is low. All security gates are satisfied.");
            case MEDIUM -> new DeploymentDecision(Decision.REVIEW_REQUIRED,
                    "Risk is medium. A security engineer must review the findings before release.");
            case HIGH -> new DeploymentDecision(Decision.BLOCKED,
                    "Risk is high. Deployment is blocked until the critical findings are fixed.");
        };
    }

    public DeploymentResult simulate(PipelineRun run) {
        String ref = run.projectName() + " (pipeline #" + run.id() + ")";
        return switch (run.decision().decision()) {
            case APPROVED -> new DeploymentResult(true, Decision.APPROVED, "Simulated deployment succeeded",
                    ref + " was deployed to the SIMULATED staging environment. Nothing was really deployed.",
                    List.of("Artifact signature verified", "Staging rollout started", "Health checks passed",
                            "Traffic switched to new version"));
            case REVIEW_REQUIRED -> new DeploymentResult(true, Decision.REVIEW_REQUIRED, "Security review required",
                    ref + " is paused. A security engineer must approve the open findings first.",
                    List.of("Review ticket created (simulated)", "Deployment paused", "Waiting for approval"));
            case BLOCKED -> new DeploymentResult(true, Decision.BLOCKED, "Deployment blocked",
                    ref + " was blocked by the adaptive policy: risk score " + run.risk().score()
                            + " (" + run.risk().level() + "). Fix the critical findings and re-run the pipeline.",
                    List.of("Policy evaluated", "Deployment rejected", "Developers notified (simulated)"));
        };
    }
}
