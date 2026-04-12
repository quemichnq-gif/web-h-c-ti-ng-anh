package com.example.demo.controller;

import com.example.demo.model.AuditLogEntry;
import com.example.demo.service.AuditLogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
public class AuditLogController {
    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/audit-logs")
    public String listAuditLogs(Model model) {
        List<AuditLogEntry> logs = auditLogService.getRecentLogs(50);
        model.addAttribute("logs", logs.stream()
                .map(this::toRow)
                .toList());
        model.addAttribute("totalLogs", auditLogService.count());
        model.addAttribute("entityTypes", logs.stream().map(AuditLogEntry::getEntityType).filter(s -> s != null && !s.isBlank()).collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new)));
        model.addAttribute("latestAction", logs.isEmpty() ? "N/A" : logs.get(0).getAction());
        model.addAttribute("latestTimestamp", logs.isEmpty() ? null : logs.get(0).getTimestamp());
        model.addAttribute("latestEntityType", logs.isEmpty() ? "N/A" : logs.get(0).getEntityType());
        return "audit-logs/list";
    }

    @PostMapping("/audit-logs/delete-all")
    public String deleteAllAuditLogs(RedirectAttributes ra) {
        auditLogService.deleteAll();
        ra.addFlashAttribute("success", "All audit logs were deleted successfully.");
        return "redirect:/audit-logs";
    }

    private AuditLogRow toRow(AuditLogEntry entry) {
        if (entry == null) {
            return new AuditLogRow(null, "N/A", null, "No additional details.", "N/A");
        }
        return new AuditLogRow(
                entry.getTimestamp(),
                entry.getAction(),
                entry.getEntityId(),
                entry.getEntityType(),
                normalizeDetails(entry.getDetails())
        );
    }

    private String normalizeDetails(String details) {
        if (details == null || details.isBlank()) {
            return "No additional details.";
        }
        String normalized = details.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) {
            return "No additional details.";
        }
        if (normalized.endsWith(".") || normalized.endsWith("!") || normalized.endsWith("?")) {
            return normalized;
        }
        return normalized + ".";
    }

    public record AuditLogRow(LocalDateTime timestamp,
                              String action,
                              Long entityId,
                              String entityType,
                              String details) {
    }
}
