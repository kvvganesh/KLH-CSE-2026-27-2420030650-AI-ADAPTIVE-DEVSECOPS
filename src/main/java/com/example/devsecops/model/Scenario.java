package com.example.devsecops.model;

public enum Scenario {
    SAFE("Safe project"), MEDIUM("Medium risk"), CRITICAL("Critical risk"), LIVE("Live local scan");

    private final String label;

    Scenario(String label) { this.label = label; }

    public String getLabel() { return label; }
}
