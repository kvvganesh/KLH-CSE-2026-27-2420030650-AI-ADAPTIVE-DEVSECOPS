package com.example.devsecops.model;

public class Project {
    private Long id;
    private String name;
    private String repositoryUrl;
    private String branch;
    private String description;
    private Scenario scenario;

    public Project(String name, String repositoryUrl, String branch, String description, Scenario scenario) {
        this.name = name;
        this.repositoryUrl = repositoryUrl;
        this.branch = branch;
        this.description = description;
        this.scenario = scenario;
    }

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }

    public String getRepositoryUrl() { return repositoryUrl; }

    public String getBranch() { return branch; }

    public String getDescription() { return description; }

    public Scenario getScenario() { return scenario; }

    public void setScenario(Scenario scenario) { this.scenario = scenario; }
}
