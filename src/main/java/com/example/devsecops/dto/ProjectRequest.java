package com.example.devsecops.dto;

import com.example.devsecops.model.Scenario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProjectRequest(
        @NotBlank(message = "Project name is required")
        @Size(max = 60, message = "Project name must be at most 60 characters")
        String name,

        @NotBlank(message = "Repository URL is required")
        @Pattern(regexp = "^(https?://\\S+|[A-Za-z]:[\\\\/].+|/.+)$",
                message = "Repository must be an http(s) URL or an absolute local folder path")
        String repositoryUrl,

        @NotBlank(message = "Branch is required")
        @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._/-]*$", message = "Branch contains invalid characters")
        @Size(max = 60, message = "Branch must be at most 60 characters")
        String branch,

        @Size(max = 300, message = "Description must be at most 300 characters")
        String description,

        Scenario scenario) {
}
