package com.quince.lawyeraiassistant.security.authorization.tool;

import java.util.Objects;

/**
 * Result produced by a Tool Authorization policy.
 *
 * @param decision   final authorization decision
 * @param toolName   tool being authorized
 * @param policyName policy that produced the decision
 * @param reason     reason for the decision
 */
public record ToolAuthorizationResult(
        ToolAuthorizationDecision decision,
        String toolName,
        String policyName,
        String reason) {

    public ToolAuthorizationResult {

        Objects.requireNonNull(
                decision,
                "decision must not be null");

        Objects.requireNonNull(
                toolName,
                "toolName must not be null");

        Objects.requireNonNull(
                policyName,
                "policyName must not be null");

        toolName = toolName.strip();

        policyName = policyName.strip();

        if (toolName.isEmpty()) {
            throw new IllegalArgumentException(
                    "toolName must not be blank");
        }

        if (policyName.isEmpty()) {
            throw new IllegalArgumentException(
                    "policyName must not be blank");
        }

        reason = reason == null
                ? ""
                : reason;
    }

    public static ToolAuthorizationResult allow(
            String toolName,
            String policyName) {

        return new ToolAuthorizationResult(
                ToolAuthorizationDecision.ALLOW,
                toolName,
                policyName,
                "");
    }

    public static ToolAuthorizationResult deny(
            String toolName,
            String policyName,
            String reason) {

        return new ToolAuthorizationResult(
                ToolAuthorizationDecision.DENY,
                toolName,
                policyName,
                reason);
    }

    public boolean isAllowed() {

        return decision == ToolAuthorizationDecision.ALLOW;
    }

    public boolean isDenied() {

        return decision == ToolAuthorizationDecision.DENY;
    }
}