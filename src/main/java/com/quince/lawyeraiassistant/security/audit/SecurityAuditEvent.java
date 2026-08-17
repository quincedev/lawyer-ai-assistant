package com.quince.lawyeraiassistant.security.audit;

import java.util.Map;
import java.util.Objects;

public record SecurityAuditEvent(
        SecurityAuditEventType type,
        SecurityAuditSeverity severity,
        String component,
        String reason,
        Map<String, String> metadata) {

    public SecurityAuditEvent {

        Objects.requireNonNull(
                type,
                "type must not be null");

        Objects.requireNonNull(
                severity,
                "severity must not be null");

        component = normalizeRequired(
                component,
                "component");

        reason = normalizeRequired(
                reason,
                "reason");

        metadata = metadata == null
                ? Map.of()
                : Map.copyOf(
                        metadata);
    }

    public static SecurityAuditEvent warn(
            SecurityAuditEventType type,
            String component,
            String reason,
            Map<String, String> metadata) {

        return new SecurityAuditEvent(
                type,
                SecurityAuditSeverity.WARN,
                component,
                reason,
                metadata);
    }

    private static String normalizeRequired(
            String value,
            String fieldName) {

        Objects.requireNonNull(
                value,
                fieldName + " must not be null");

        String normalized = value.trim();

        if (normalized.isBlank()) {

            throw new IllegalArgumentException(
                    fieldName + " must not be blank");
        }

        return normalized;
    }
}