package com.quince.lawyeraiassistant.security.audit;

import com.quince.lawyeraiassistant.security.metrics.SecurityMetricsRecorder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DefaultSecurityAuditLoggerTest {

    private SecurityMetricsRecorder securityMetricsRecorder;

    private DefaultSecurityAuditLogger auditLogger;

    @BeforeEach
    void setUp() {

        securityMetricsRecorder = mock(
                SecurityMetricsRecorder.class);

        auditLogger = new DefaultSecurityAuditLogger(
                securityMetricsRecorder);
    }

    @Test
    void shouldRecordMetricWhenSecurityEventIsLogged() {

        SecurityAuditEvent event = new SecurityAuditEvent(
                SecurityAuditEventType.INPUT_GUARDRAIL_BLOCKED,
                SecurityAuditSeverity.WARN,
                "InputGuardrailService",
                "Input rejected",
                Map.of());

        auditLogger.log(
                event);

        verify(
                securityMetricsRecorder)
                .record(
                        event);
    }
}