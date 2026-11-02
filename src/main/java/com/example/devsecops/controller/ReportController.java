package com.example.devsecops.controller;

import com.example.devsecops.model.PipelineRun;
import com.example.devsecops.service.PipelineService;
import com.example.devsecops.service.ProjectService;
import com.example.devsecops.service.ReportService;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReportController {

    private final ProjectService projects;
    private final PipelineService pipelines;
    private final ReportService reports;

    public ReportController(ProjectService projects, PipelineService pipelines, ReportService reports) {
        this.projects = projects;
        this.pipelines = pipelines;
        this.reports = reports;
    }

    @GetMapping("/api/reports")
    public List<PipelineRun> list() {
        return pipelines.all();
    }

    @GetMapping("/api/reports/{runId}/html")
    public ResponseEntity<String> byRun(@PathVariable long runId, @RequestParam(defaultValue = "false") boolean download) {
        return html(pipelines.get(runId), download);
    }

    @GetMapping("/api/projects/{id}/report")
    public ResponseEntity<String> latest(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean download) {
        return html(pipelines.requireLatest(projects.get(id).getId()), download);
    }

    private ResponseEntity<String> html(PipelineRun run, boolean download) {
        ResponseEntity.BodyBuilder response = ResponseEntity.ok().contentType(MediaType.parseMediaType("text/html;charset=UTF-8"));
        if (download) {
            response.header(HttpHeaders.CONTENT_DISPOSITION,
                    ContentDisposition.attachment().filename("security-report-pipeline-" + run.id() + ".html").build().toString());
        }
        return response.body(reports.render(run));
    }
}
