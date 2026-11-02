package com.example.devsecops.controller;

import com.example.devsecops.dto.DeploymentResult;
import com.example.devsecops.model.DeploymentDecision;
import com.example.devsecops.service.DeploymentService;
import com.example.devsecops.service.PipelineService;
import com.example.devsecops.service.ProjectService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DeploymentController {

    private final ProjectService projects;
    private final PipelineService pipelines;
    private final DeploymentService deployments;

    public DeploymentController(ProjectService projects, PipelineService pipelines, DeploymentService deployments) {
        this.projects = projects;
        this.pipelines = pipelines;
        this.deployments = deployments;
    }

    @GetMapping("/api/projects/{id}/deployment")
    public DeploymentDecision decision(@PathVariable Long id) {
        return pipelines.requireLatest(projects.get(id).getId()).decision();
    }

    @PostMapping("/api/projects/{id}/deploy")
    public DeploymentResult deploy(@PathVariable Long id) {
        return deployments.simulate(pipelines.requireLatest(projects.get(id).getId()));
    }
}
