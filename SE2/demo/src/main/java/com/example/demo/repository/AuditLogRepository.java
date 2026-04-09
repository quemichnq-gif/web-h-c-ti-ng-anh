package com.example.demo.repository;

import com.example.demo.model.AuditLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLogEntry, Long> {
    List<AuditLogEntry> findTop100ByOrderByTimestampDesc();
}
