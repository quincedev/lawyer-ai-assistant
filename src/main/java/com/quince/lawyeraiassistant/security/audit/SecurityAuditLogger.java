package com.quince.lawyeraiassistant.security.audit;

public interface SecurityAuditLogger {

    void log(
            SecurityAuditEvent event);
}