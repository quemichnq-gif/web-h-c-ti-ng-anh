package com.example.demo.model;

import java.time.LocalDateTime;

public class AuditLogEntry {
    private final LocalDateTime timestamp;
    private final String action;
    private final String entityType;
    private final Long entityId;
    private final String details;

    public AuditLogEntry(LocalDateTime timestamp, String action, String entityType, Long entityId, String details) {
        this.timestamp = timestamp;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.details = details;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getAction() {
        return action;
    }

    public String getEntityType() {
        return entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public String getDetails() {
        return details;
    }
}
