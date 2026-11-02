package com.example.devsecops.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Risk thresholds: score below mediumThreshold = LOW, below highThreshold = MEDIUM, otherwise HIGH. */
@ConfigurationProperties(prefix = "devsecops.risk")
public record RiskProperties(@DefaultValue("30") int mediumThreshold,
                             @DefaultValue("60") int highThreshold) {
}
