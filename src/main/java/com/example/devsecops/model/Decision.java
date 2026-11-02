package com.example.devsecops.model;

public enum Decision {
    APPROVED("DEPLOYMENT APPROVED", "✓"),
    REVIEW_REQUIRED("SECURITY REVIEW REQUIRED", "⚠"),
    BLOCKED("DEPLOYMENT BLOCKED", "✕");

    private final String label;
    private final String icon;

    Decision(String label, String icon) { this.label = label; this.icon = icon; }

    public String getLabel() { return label; }

    public String getIcon() { return icon; }
}
