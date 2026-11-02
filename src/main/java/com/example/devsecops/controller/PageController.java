package com.example.devsecops.controller;

import com.example.devsecops.config.RiskProperties;
import com.example.devsecops.config.ScanProperties;
import com.example.devsecops.exception.NotFoundException;
import com.example.devsecops.model.PipelineRun;
import com.example.devsecops.model.Project;
import com.example.devsecops.model.Scenario;
import com.example.devsecops.service.PipelineService;
import com.example.devsecops.service.ProjectService;
import com.example.devsecops.service.RiskAssessmentService;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Server-rendered pages (Thymeleaf). Data changes go through the REST API from app.js. */
@Controller
public class PageController {

    private final ProjectService projects;
    private final PipelineService pipelines;
    private final RiskAssessmentService risk;
    private final RiskProperties properties;
    private final ScanProperties scanProperties;

    public PageController(ProjectService projects, PipelineService pipelines, RiskAssessmentService risk,
                          RiskProperties properties, ScanProperties scanProperties) {
        this.scanProperties = scanProperties;
        this.projects = projects;
        this.pipelines = pipelines;
        this.risk = risk;
        this.properties = properties;
    }

    @ModelAttribute("allProjects")
    public List<Project> allProjects() {
        return projects.findAll();
    }

    @ModelAttribute("scenarios")
    public Scenario[] scenarios() {
        return Scenario.values();
    }

    @GetMapping({"/", "/login"})
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("stats", pipelines.stats(projects.findAll().size()));
        model.addAttribute("recentRuns", pipelines.recent(8));
        return "dashboard";
    }

    @GetMapping("/projects")
    public String projects(Model model) {
        model.addAttribute("summaries", projects.summaries());
        return "projects";
    }

    @GetMapping("/projects/{id}")
    public String projectDetails(@PathVariable Long id, Model model) {
        model.addAttribute("summary", projects.summary(id));
        model.addAttribute("history", pipelines.history(id));
        return "project-details";
    }

    @GetMapping("/pipelines")
    public String pipeline(@RequestParam(required = false) Long project, Model model) {
        PipelineRun run = scope(model, project);
        model.addAttribute("stages", run != null ? run.stages() : pipelines.pendingStages());
        return "pipeline";
    }

    @GetMapping("/security-scans")
    public String securityScans(@RequestParam(required = false) Long project, Model model) {
        scope(model, project);
        return "security-scans";
    }

    @GetMapping("/vulnerabilities")
    public String vulnerabilities(@RequestParam(required = false) Long project, Model model) {
        scope(model, project);
        return "vulnerabilities";
    }

    @GetMapping("/risk")
    public String riskAnalysis(@RequestParam(required = false) Long project, Model model) {
        scope(model, project);
        model.addAttribute("rules", risk.rules());
        model.addAttribute("thresholds", properties);
        return "risk-analysis";
    }

    @GetMapping("/deployments")
    public String deployments(@RequestParam(required = false) Long project, Model model) {
        scope(model, project);
        return "deployment";
    }

    @GetMapping("/reports")
    public String reports(Model model) {
        model.addAttribute("runs", pipelines.all());
        return "reports";
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("thresholds", properties);
        model.addAttribute("rules", risk.rules());
        model.addAttribute("depCheckEnabled", scanProperties.dependencyCheckEnabled());
        model.addAttribute("sonarConfigured", !scanProperties.sonarUrl().isBlank() && !scanProperties.sonarToken().isBlank());
        return "settings";
    }

    @GetMapping("/documentation")
    public String documentation() {
        return "documentation";
    }

    /** Resolves the selected project (default: first) and its latest run into the model. */
    private PipelineRun scope(Model model, Long projectId) {
        Project selected = projectId != null ? projects.get(projectId)
                : projects.findAll().stream().findFirst().orElseThrow(() -> new NotFoundException("No projects exist yet."));
        PipelineRun run = pipelines.latest(selected.getId()).orElse(null);
        model.addAttribute("selected", selected);
        model.addAttribute("run", run);
        model.addAttribute("history", pipelines.history(selected.getId()));
        return run;
    }
}
