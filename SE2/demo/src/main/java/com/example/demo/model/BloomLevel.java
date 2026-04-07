package com.example.demo.model;

public enum BloomLevel {
    REMEMBER("Remember"),
    UNDERSTAND("Understand"),
    APPLY("Apply"),
    ANALYZE("Analyze"),
    EVALUATE("Evaluate"),
    CREATE("Create");

    private final String label;

    BloomLevel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
