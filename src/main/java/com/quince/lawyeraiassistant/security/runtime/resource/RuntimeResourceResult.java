package com.quince.lawyeraiassistant.security.runtime.resource;

import java.util.Objects;

public record RuntimeResourceResult(
        RuntimeResourceDecision decision,
        String policyName,
        String reason) {

    public RuntimeResourceResult {

        Objects.requireNonNull(
                decision,
                "decision must not be null");

        Objects.requireNonNull(
                policyName,
                "policyName must not be null");

        policyName = policyName.strip();

        if (policyName.isEmpty()) {

            throw new IllegalArgumentException(
                    "policyName must not be blank");
        }

        reason = reason == null
                ? ""
                : reason.strip();
    }

    public static RuntimeResourceResult allow(
            String policyName) {

        return new RuntimeResourceResult(
                RuntimeResourceDecision.ALLOW,
                policyName,
                "");
    }

    public static RuntimeResourceResult deny(
            String policyName,
            String reason) {

        return new RuntimeResourceResult(
                RuntimeResourceDecision.DENY,
                policyName,
                reason);
    }

    public boolean isAllowed() {

        return decision == RuntimeResourceDecision.ALLOW;
    }

    public boolean isDenied() {

        return decision == RuntimeResourceDecision.DENY;
    }
}