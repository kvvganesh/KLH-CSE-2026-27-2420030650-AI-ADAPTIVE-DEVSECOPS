package com.example.devsecops.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Settings for live local scans. Blank values mean "not configured". */
@ConfigurationProperties(prefix = "devsecops.scan")
public record ScanProperties(
        @DefaultValue("") String workspace,
        @DefaultValue("gitleaks") String gitleaksCommand,
        @DefaultValue("") String mavenCommand,
        @DefaultValue("15") int timeoutMinutes,
        @DefaultValue("false") boolean dependencyCheckEnabled,
        @DefaultValue("60") int dependencyCheckTimeoutMinutes,
        @DefaultValue("") String nvdApiKey,
        @DefaultValue("") String sonarUrl,
        @DefaultValue("") String sonarToken) {
}
