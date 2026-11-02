package com.example.devsecops.dto;

import com.example.devsecops.model.PipelineRun;
import com.example.devsecops.model.Project;

/** A project plus its most recent pipeline run (lastRun is null if it never ran). */
public record ProjectSummary(Project project, PipelineRun lastRun) {
}
