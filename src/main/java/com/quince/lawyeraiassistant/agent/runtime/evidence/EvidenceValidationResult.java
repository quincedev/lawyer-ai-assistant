package com.quince.lawyeraiassistant.agent.runtime.evidence;

import java.util.Objects;

public record EvidenceValidationResult(
        boolean accepted,
        String reason) {

    public EvidenceValidationResult {

        if (accepted) {

            reason = null;

        } else {

            reason = Objects.requireNonNull(
                    reason,
                    "reason must not be null");

            if (reason.isBlank()) {
                throw new IllegalArgumentException(
                        "reason must not be blank");
            }
        }
    }

    public static EvidenceValidationResult allow() {

        return new EvidenceValidationResult(
                true,
                null);
    }

    public static EvidenceValidationResult reject(
            String reason) {

        return new EvidenceValidationResult(
                false,
                reason);
    }
}