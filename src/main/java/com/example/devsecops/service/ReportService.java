package com.example.devsecops.service;

import com.example.devsecops.model.PipelineRun;
import com.example.devsecops.model.RiskFactor;
import com.example.devsecops.model.SecurityScan;
import com.example.devsecops.model.Vulnerability;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

/** Builds a standalone, printable HTML security report for one pipeline run. */
@Service
public class ReportService {

    public String render(PipelineRun run) {
        SecurityScan s = run.scan();
        String color = switch (run.risk().level()) {
            case LOW -> "#15803d";
            case MEDIUM -> "#b45309";
            case HIGH -> "#b91c1c";
        };

        StringBuilder factors = new StringBuilder();
        for (RiskFactor f : run.risk().factors()) {
            factors.append("<tr><td>").append(esc(f.description())).append("</td><td>+").append(f.points()).append("</td></tr>");
        }
        if (run.risk().factors().isEmpty()) {
            factors.append("<tr><td colspan=\"2\">No risk factors detected</td></tr>");
        }
        StringBuilder findings = new StringBuilder();
        for (Vulnerability v : s.findings()) {
            findings.append("<tr><td>").append(esc(v.issue())).append("</td><td>").append(v.severity())
                    .append("</td><td>").append(esc(v.tool())).append("</td><td>").append(esc(v.status()))
                    .append("</td><td>").append(esc(v.recommendation())).append("</td></tr>");
        }
        if (s.findings().isEmpty()) {
            findings.append("<tr><td colspan=\"5\">No findings</td></tr>");
        }

        return "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\">"
                + "<title>Security report - pipeline #" + run.id() + "</title><style>"
                + "body{font-family:Segoe UI,Arial,sans-serif;max-width:860px;margin:32px auto;color:#111;padding:0 16px}"
                + "h1{margin-bottom:0}h2{margin-top:28px;border-bottom:1px solid #ccc;padding-bottom:4px}"
                + "table{border-collapse:collapse;width:100%}td,th{border:1px solid #ccc;padding:6px 10px;text-align:left}"
                + "th{background:#f1f5f9}.badge{display:inline-block;padding:4px 14px;border-radius:6px;color:#fff;font-weight:700;background:"
                + color + "}.muted{color:#555}</style></head><body>"
                + "<h1>Security Assessment Report</h1>"
                + "<p class=\"muted\">Adaptive DevSecOps Platform - simulated demonstration data</p>"
                + "<table><tr><th>Project</th><td>" + esc(run.projectName()) + "</td><th>Pipeline ID</th><td>#" + run.id() + "</td></tr>"
                + "<tr><th>Date</th><td>" + esc(run.startedAt()) + "</td><th>Pipeline status</th><td>" + run.status() + "</td></tr></table>"
                + "<h2>Decision</h2><p><span class=\"badge\">" + run.risk().level() + " RISK - score " + run.risk().score()
                + "</span> &nbsp; <strong>" + run.decision().decision().getLabel() + "</strong></p>"
                + "<p>" + esc(run.decision().message()) + " Recommended action: <strong>" + esc(run.risk().recommendedAction()) + "</strong></p>"
                + "<h2>Scanner results</h2><table><tr><th>Tool</th><th>Result</th></tr>"
                + "<tr><td>JUnit</td><td>" + s.testsPassed() + " of " + s.testsTotal() + " tests passed, " + s.testsFailed() + " failed</td></tr>"
                + "<tr><td>SonarQube</td><td>Quality gate " + (s.qualityGatePassed() ? "PASSED" : "FAILED") + " - " + s.bugs() + " bugs, "
                + s.vulnerabilities() + " vulnerabilities, " + s.codeSmells() + " code smells</td></tr>"
                + "<tr><td>Gitleaks</td><td>" + s.secrets() + " secrets detected</td></tr>"
                + "<tr><td>OWASP Dependency-Check</td><td>" + s.dependenciesScanned() + " dependencies scanned: " + s.depCritical()
                + " critical, " + s.depHigh() + " high, " + s.depMedium() + " medium, " + s.depLow() + " low</td></tr>" + skippedRows(s) + "</table>"
                + "<h2>Risk factors</h2><table><tr><th>Reason</th><th>Points</th></tr>" + factors + "</table>"
                + "<h2>Vulnerability assessment</h2><table><tr><th>Issue</th><th>Severity</th><th>Tool</th><th>Status</th><th>Recommendation</th></tr>"
                + findings + "</table></body></html>";
    }

    private String skippedRows(SecurityScan s) {
        StringBuilder sb = new StringBuilder();
        s.skipped().forEach((tool, reason) -> sb.append("<tr><td>").append(esc(tool))
                .append("</td><td>NOT SCANNED: ").append(esc(reason)).append("</td></tr>"));
        return sb.toString();
    }

    private String esc(String text) {
        return HtmlUtils.htmlEscape(text == null ? "" : text);
    }
}
