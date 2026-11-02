package com.example.devsecops.controller;

import com.example.devsecops.model.PipelineRun;
import com.example.devsecops.service.PipelineService;
import com.example.devsecops.service.ProjectService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PipelineController {

    private final ProjectService projects;
    private final PipelineService pipelines;

    public PipelineController(ProjectService projects, PipelineService pipelines) {
        this.projects = projects;
        this.pipelines = pipelines;
    }

    @PostMapping("/api/projects/{id}/pipeline")
    @ResponseStatus(HttpStatus.CREATED)
    public PipelineRun run(@PathVariable Long id) {
        return pipelines.run(projects.get(id));
    }

    @GetMapping("/api/projects/{id}/pipelines")
    public List<PipelineRun> history(@PathVariable Long id) {
        return pipelines.history(projects.get(id).getId());
    }

    @GetMapping("/api/pipelines")
    public List<PipelineRun> all() {
        return pipelines.all();
    }
}
