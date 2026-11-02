package com.example.devsecops.config;

import com.example.devsecops.dto.ProjectRequest;
import com.example.devsecops.model.Project;
import com.example.devsecops.model.Scenario;
import com.example.devsecops.service.PipelineService;
import com.example.devsecops.service.ProjectService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Loads demo projects and a short pipeline history so the UI is never empty. */
@Configuration
public class DataInitializer {

    @Bean
    ApplicationRunner seedDemoData(ProjectService projects, PipelineService pipelines) {
        return args -> {
            seed(projects, pipelines, "Payment API", "https://github.com/demo-org/payment-api", "main",
                    "Handles card payments and refunds.", Scenario.MEDIUM, Scenario.SAFE);
            seed(projects, pipelines, "Inventory Service", "https://github.com/demo-org/inventory-service", "main",
                    "Stock levels and warehouse synchronisation.", Scenario.SAFE, Scenario.MEDIUM);
            seed(projects, pipelines, "Customer API", "https://github.com/demo-org/customer-api", "develop",
                    "Customer profiles and authentication.", Scenario.MEDIUM, Scenario.CRITICAL);
        };
    }

    private void seed(ProjectService projects, PipelineService pipelines, String name, String repo, String branch,
                      String description, Scenario first, Scenario second) {
        Project p = projects.create(new ProjectRequest(name, repo, branch, description, first));
        pipelines.run(p);
        projects.setScenario(p.getId(), second);
        pipelines.run(p);
    }
}
