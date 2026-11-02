package com.example.devsecops.controller;

import com.example.devsecops.dto.ProjectRequest;
import com.example.devsecops.dto.ProjectSummary;
import com.example.devsecops.dto.ScenarioRequest;
import com.example.devsecops.model.Project;
import com.example.devsecops.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projects;

    public ProjectController(ProjectService projects) {
        this.projects = projects;
    }

    @GetMapping
    public List<ProjectSummary> list() {
        return projects.summaries();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Project create(@Valid @RequestBody ProjectRequest request) {
        return projects.create(request);
    }

    @GetMapping("/{id}")
    public ProjectSummary get(@PathVariable Long id) {
        return projects.summary(id);
    }

    /** Switches the demo scenario (SAFE / MEDIUM / CRITICAL) used by the next pipeline run. */
    @PutMapping("/{id}/scenario")
    public Project setScenario(@PathVariable Long id, @Valid @RequestBody ScenarioRequest request) {
        return projects.setScenario(id, request.scenario());
    }
}
