package com.example.devsecops.dto;

import com.example.devsecops.model.Decision;
import java.util.List;

/** Outcome of a SIMULATED deployment. Nothing is ever deployed. */
public record DeploymentResult(boolean simulated, Decision outcome, String title, String message, List<String> steps) {
}
