package com.quince.lawyeraiassistant.security.guardrail;

import java.util.Objects;

/**
 * Represents the result produced by a guardrail evaluation.
 *
 * @param decision      the guardrail decision
 * @param guardrailName the name of the guardrail that produced the result
 * @param reason        the reason for the decision
 */
public record GuardrailResult(
        GuardrailDecision decision,
        String guardrailName,
        String reason) {

    public GuardrailResult {

        Objects.requireNonNull(
                decision,
                "decision must not be null");

        Objects.requireNonNull(
                guardrailName,
                "guardrailName must not be null");

        reason = reason == null
                ? ""
                : reason;
    }

    /**
     * Creates an ALLOW result.
     */
    public static GuardrailResult allow(
            String guardrailName) {

        return new GuardrailResult(
                GuardrailDecision.ALLOW,
                guardrailName,
                "");
    }

    /**
     * Creates a BLOCK result.
     */
    public static GuardrailResult block(
            String guardrailName,
            String reason) {

        return new GuardrailResult(
                GuardrailDecision.BLOCK,
                guardrailName,
                reason);
    }

    /**
     * Returns whether this result allows execution to continue.
     */
    public boolean isAllowed() {

        return decision == GuardrailDecision.ALLOW;
    }

    /**
     * Returns whether this result blocks execution.
     */
    public boolean isBlocked() {

        return decision == GuardrailDecision.BLOCK;
    }
}