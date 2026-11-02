package com.example.devsecops.controller;

import com.example.devsecops.model.SecurityScan;
import com.example.devsecops.model.Severity;
import com.example.devsecops.model.Vulnerability;
import com.example.devsecops.service.PipelineService;
import com.example.devsecops.service.ProjectService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SecurityScanController {

    private final ProjectService projects;
    private final PipelineService pipelines;

    public SecurityScanController(ProjectService projects, PipelineService pipelines) {
        this.projects = projects;
        this.pipelines = pipelines;
    }

    @GetMapping("/api/projects/{id}/security-scans")
    public SecurityScan scan(@PathVariable Long id) {
        return pipelines.requireLatest(projects.get(id).getId()).scan();
    }

    @GetMapping("/api/projects/{id}/vulnerabilities")
    public List<Vulnerability> vulnerabilities(@PathVariable Long id, @RequestParam(required = false) Severity severity) {
        List<Vulnerability> all = pipelines.requireLatest(projects.get(id).getId()).scan().findings();
        return severity == null ? all : all.stream().filter(v -> v.severity() == severity).toList();
    }
}
