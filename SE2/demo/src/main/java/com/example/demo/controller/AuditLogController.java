package com.example.demo.controller;

import com.example.demo.model.AuditLogEntry;
import com.example.demo.service.AuditLogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Set;

@Controller
public class AuditLogController {
    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/audit-logs")
    public String listAuditLogs(Model model) {
        List<AuditLogEntry> logs = auditLogService.getRecentLogs(50);
        model.addAttribute("logs", logs);
        model.addAttribute("totalLogs", auditLogService.count());
        model.addAttribute("entityTypes", logs.stream().map(AuditLogEntry::getEntityType).filter(s -> s != null && !s.isBlank()).collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new)));
        model.addAttribute("latestAction", logs.isEmpty() ? "N/A" : logs.get(0).getAction());
        model.addAttribute("latestTimestamp", logs.isEmpty() ? null : logs.get(0).getTimestamp());
        model.addAttribute("latestEntityType", logs.isEmpty() ? "N/A" : logs.get(0).getEntityType());
        return "audit-logs/list";
    }
}
