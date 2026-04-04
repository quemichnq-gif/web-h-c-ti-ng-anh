package com.example.demo.service;

import com.example.demo.model.AuditLogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class AuditLogService {
    private static final int MAX_ENTRIES = 100;
    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);
    private final List<AuditLogEntry> entries = new ArrayList<>();

    public synchronized void log(String action, String entityType, Long entityId, String details) {
        AuditLogEntry entry = new AuditLogEntry(
                LocalDateTime.now(),
                action,
                entityType,
                entityId,
                details
        );
        entries.add(0, entry);
        if (entries.size() > MAX_ENTRIES) {
            entries.remove(entries.size() - 1);
        }
        log.info("AUDIT action={} entityType={} entityId={} details={}",
                action, entityType, entityId, details);
    }

    public synchronized List<AuditLogEntry> getRecentLogs() {
        return new ArrayList<>(entries);
    }

    public synchronized List<AuditLogEntry> getRecentLogs(int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        return entries.stream().limit(limit).toList();
    }

    public synchronized long count() {
        return entries.size();
    }
}
