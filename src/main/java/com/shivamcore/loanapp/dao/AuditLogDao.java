package com.shivamcore.loanapp.dao;

import com.shivamcore.loanapp.model.AuditLog;

import java.util.List;

public interface AuditLogDao {
    AuditLog save(AuditLog auditLog);
    List<AuditLog> findRecent(int limit);
}
