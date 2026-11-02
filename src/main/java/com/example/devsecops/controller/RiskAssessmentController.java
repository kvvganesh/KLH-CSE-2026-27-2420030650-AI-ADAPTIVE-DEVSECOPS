package com.example.devsecops.controller;

import com.example.devsecops.model.RiskAssessment;
import com.example.devsecops.service.PipelineService;
import com.example.devsecops.service.ProjectService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RiskAssessmentController {

    private final ProjectService projects;
    private final PipelineService pipelines;

    public RiskAssessmentController(ProjectService projects, PipelineService pipelines) {
        this.projects = projects;
        this.pipelines = pipelines;
    }

    @GetMapping("/api/projects/{id}/risk")
    public RiskAssessment risk(@PathVariable Long id) {
        return pipelines.requireLatest(projects.get(id).getId()).risk();
    }
}
