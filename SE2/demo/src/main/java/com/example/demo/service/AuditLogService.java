package com.example.demo.service;

import com.example.demo.model.AuditLogEntry;
import com.example.demo.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class AuditLogService {
    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);
    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public synchronized void log(String action, String entityType, Long entityId, String details) {
        AuditLogEntry entry = new AuditLogEntry(
                LocalDateTime.now(),
                action,
                entityType,
                entityId,
                details
        );
        auditLogRepository.save(entry);
        log.info("AUDIT action={} entityType={} entityId={} details={}",
                action, entityType, entityId, details);
    }

    public synchronized List<AuditLogEntry> getRecentLogs() {
        return auditLogRepository.findTop100ByOrderByTimestampDesc();
    }

    public synchronized List<AuditLogEntry> getRecentLogs(int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        return auditLogRepository.findAll().stream()
                .sorted((left, right) -> right.getTimestamp().compareTo(left.getTimestamp()))
                .limit(limit)
                .toList();
    }

    public synchronized long count() {
        return auditLogRepository.count();
    }

    public synchronized void deleteAll() {
        auditLogRepository.deleteAllInBatch();
    }
}
