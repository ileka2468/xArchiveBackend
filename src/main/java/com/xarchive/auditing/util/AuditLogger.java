package com.xarchive.auditing.util;

import com.xarchive.auditing.entity.AuditLog;
import com.xarchive.auditing.repository.AuditLogRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class AuditLogger {

    private final AuditLogRepository auditLogRepository;

    public AuditLogger(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(String action, String type, String customerID, String remarks) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setType(type);
        log.setPerformedBy(customerID);
        log.setPerformedAt(Instant.now());
        log.setRemarks(remarks);

        auditLogRepository.save(log);
    }

    public void log(String action, String type, String customerID) {
        log(action, type, customerID, null);
    }
}
