package com.quince.lawyeraiassistant.security.metrics;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.quince.lawyeraiassistant.security.audit.SecurityAuditEvent;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Records low-cardinality security metrics.
 *
 * <p>
 * Important:
 * security metrics must never use high-cardinality values such as:
 * taskId, requestId, reason, prompt, evidence or tool arguments.
 * </p>
 */
@Component
public final class MicrometerSecurityMetricsRecorder
        implements SecurityMetricsRecorder {

    private static final String METRIC_NAME = "lawyer.ai.security.events";

    private final MeterRegistry meterRegistry;

    public MicrometerSecurityMetricsRecorder(
            MeterRegistry meterRegistry) {

        this.meterRegistry = Objects.requireNonNull(
                meterRegistry,
                "meterRegistry must not be null");
    }

    @Override
    public void record(
            SecurityAuditEvent event) {

        Objects.requireNonNull(
                event,
                "SecurityAuditEvent must not be null");

        Counter.builder(
                METRIC_NAME)
                .description(
                        "Number of AI security events")
                .tag(
                        "type",
                        event.type().name())
                .tag(
                        "severity",
                        event.severity().name())
                .register(
                        meterRegistry)
                .increment();
    }
}