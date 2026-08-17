package com.quince.lawyeraiassistant.security.runtime;

import java.util.Objects;

/**
 * Result returned by a runtime guardrail policy.
 *
 * @param decision   allow or deny decision
 * @param policyName policy that produced the decision
 * @param reason     human-readable decision reason
 */
public record RuntimeGuardrailResult(
        RuntimeGuardrailDecision decision,
        String policyName,
        String reason) {

    public RuntimeGuardrailResult {

        Objects.requireNonNull(
                decision,
                "decision must not be null");

        Objects.requireNonNull(
                policyName,
                "policyName must not be null");

        policyName = policyName.trim();

        if (policyName.isEmpty()) {
            throw new IllegalArgumentException(
                    "policyName must not be blank");
        }

        reason = reason == null
                ? ""
                : reason.trim();
    }

    public static RuntimeGuardrailResult allow(
            String policyName) {

        return new RuntimeGuardrailResult(
                RuntimeGuardrailDecision.ALLOW,
                policyName,
                "");
    }

    public static RuntimeGuardrailResult deny(
            String policyName,
            String reason) {

        return new RuntimeGuardrailResult(
                RuntimeGuardrailDecision.DENY,
                policyName,
                reason);
    }

    public boolean isAllowed() {

        return decision == RuntimeGuardrailDecision.ALLOW;
    }

    public boolean isDenied() {

        return decision == RuntimeGuardrailDecision.DENY;
    }
}