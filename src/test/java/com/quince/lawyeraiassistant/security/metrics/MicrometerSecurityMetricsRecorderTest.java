package com.quince.lawyeraiassistant.security.metrics;

import com.quince.lawyeraiassistant.security.audit.SecurityAuditEvent;
import com.quince.lawyeraiassistant.security.audit.SecurityAuditEventType;
import com.quince.lawyeraiassistant.security.audit.SecurityAuditSeverity;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MicrometerSecurityMetricsRecorderTest {

    private SimpleMeterRegistry meterRegistry;

    private MicrometerSecurityMetricsRecorder recorder;

    @BeforeEach
    void setUp() {

        meterRegistry = new SimpleMeterRegistry();

        recorder = new MicrometerSecurityMetricsRecorder(
                meterRegistry);
    }

    @Test
    void shouldIncrementSecurityEventCounter() {

        SecurityAuditEvent event = new SecurityAuditEvent(
                SecurityAuditEventType.INPUT_GUARDRAIL_BLOCKED,
                SecurityAuditSeverity.WARN,
                "InputGuardrailService",
                "Input rejected",
                Map.of());

        recorder.record(
                event);

        assertEquals(
                1.0,
                meterRegistry
                        .get(
                                "lawyer.ai.security.events")
                        .tag(
                                "type",
                                SecurityAuditEventType.INPUT_GUARDRAIL_BLOCKED
                                        .name())
                        .tag(
                                "severity",
                                SecurityAuditSeverity.WARN
                                        .name())
                        .counter()
                        .count());
    }

    @Test
    void shouldIncrementSameCounterMultipleTimes() {

        SecurityAuditEvent event = new SecurityAuditEvent(
                SecurityAuditEventType.INPUT_GUARDRAIL_BLOCKED,
                SecurityAuditSeverity.WARN,
                "InputGuardrailService",
                "Input rejected",
                Map.of());

        recorder.record(
                event);

        recorder.record(
                event);

        assertEquals(
                2.0,
                meterRegistry
                        .get(
                                "lawyer.ai.security.events")
                        .tag(
                                "type",
                                SecurityAuditEventType.INPUT_GUARDRAIL_BLOCKED
                                        .name())
                        .tag(
                                "severity",
                                SecurityAuditSeverity.WARN
                                        .name())
                        .counter()
                        .count());
    }

    @Test
    void shouldSeparateCountersByEventType() {

        SecurityAuditEvent inputBlocked = new SecurityAuditEvent(
                SecurityAuditEventType.INPUT_GUARDRAIL_BLOCKED,
                SecurityAuditSeverity.WARN,
                "InputGuardrailService",
                "Input rejected",
                Map.of());

        SecurityAuditEvent toolDenied = new SecurityAuditEvent(
                SecurityAuditEventType.TOOL_AUTHORIZATION_DENIED,
                SecurityAuditSeverity.WARN,
                "ToolAuthorizationService",
                "Tool denied",
                Map.of());

        recorder.record(
                inputBlocked);

        recorder.record(
                toolDenied);

        assertEquals(
                1.0,
                meterRegistry
                        .get(
                                "lawyer.ai.security.events")
                        .tag(
                                "type",
                                SecurityAuditEventType.INPUT_GUARDRAIL_BLOCKED
                                        .name())
                        .counter()
                        .count());

        assertEquals(
                1.0,
                meterRegistry
                        .get(
                                "lawyer.ai.security.events")
                        .tag(
                                "type",
                                SecurityAuditEventType.TOOL_AUTHORIZATION_DENIED
                                        .name())
                        .counter()
                        .count());
    }
}