package com.quince.lawyeraiassistant.security.metrics;

import com.quince.lawyeraiassistant.security.audit.SecurityAuditEvent;

public interface SecurityMetricsRecorder {

    void record(
            SecurityAuditEvent event);
}