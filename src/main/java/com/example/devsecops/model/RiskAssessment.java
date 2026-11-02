package com.example.devsecops.model;

import java.util.List;

public record RiskAssessment(int score, RiskLevel level, List<RiskFactor> factors, String recommendedAction) {
}
