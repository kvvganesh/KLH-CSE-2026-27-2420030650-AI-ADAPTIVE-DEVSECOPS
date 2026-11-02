package com.example.devsecops.service;

import com.example.devsecops.dto.ProjectRequest;
import com.example.devsecops.dto.ProjectSummary;
import com.example.devsecops.exception.ConflictException;
import com.example.devsecops.exception.NotFoundException;
import com.example.devsecops.model.Project;
import com.example.devsecops.model.Scenario;
import com.example.devsecops.repository.PipelineRunRepository;
import com.example.devsecops.repository.ProjectRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProjectService {

    private final ProjectRepository projects;
    private final PipelineRunRepository runs;

    public ProjectService(ProjectRepository projects, PipelineRunRepository runs) {
        this.projects = projects;
        this.runs = runs;
    }

    public List<Project> findAll() {
        return projects.findAll();
    }

    public Project get(Long id) {
        return projects.findById(id).orElseThrow(() -> new NotFoundException("Project " + id + " was not found."));
    }

    public Project create(ProjectRequest request) {
        String name = request.name().trim();
        boolean duplicate = projects.findAll().stream().anyMatch(p -> p.getName().equalsIgnoreCase(name));
        if (duplicate) {
            throw new ConflictException("A project named '" + name + "' already exists.");
        }
        Scenario scenario = request.scenario() == null ? Scenario.SAFE : request.scenario();
        String description = request.description() == null ? "" : request.description().trim();
        return projects.save(new Project(name, request.repositoryUrl().trim(), request.branch().trim(), description, scenario));
    }

    public Project setScenario(Long id, Scenario scenario) {
        Project project = get(id);
        project.setScenario(scenario);
        return project;
    }

    public List<ProjectSummary> summaries() {
        return projects.findAll().stream().map(this::toSummary).toList();
    }

    public ProjectSummary summary(Long id) {
        return toSummary(get(id));
    }

    private ProjectSummary toSummary(Project p) {
        return new ProjectSummary(p, runs.findLatest(p.getId()).orElse(null));
    }
}
