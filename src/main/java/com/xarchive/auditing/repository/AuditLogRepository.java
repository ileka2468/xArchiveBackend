package com.xarchive.auditing.repository;

import com.xarchive.auditing.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Integer> {
    // Additional custom queries can be added here if needed
}
