package com.example.devsecops.dto;

import com.example.devsecops.model.Scenario;
import jakarta.validation.constraints.NotNull;

public record ScenarioRequest(@NotNull(message = "Scenario is required") Scenario scenario) {
}
