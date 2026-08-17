package com.quince.lawyeraiassistant.security.mcp;

import java.util.Objects;

/**
 * Immutable result produced by an MCP Tool security policy.
 *
 * @param decision   security decision
 * @param toolName   MCP Tool being evaluated
 * @param policyName policy that produced the decision
 * @param reason     optional explanation, primarily used for DENY
 */
public record McpToolSecurityResult(
        McpToolSecurityDecision decision,
        String toolName,
        String policyName,
        String reason) {

    public McpToolSecurityResult {

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
                : reason.strip();
    }

    public static McpToolSecurityResult allow(
            String toolName,
            String policyName) {

        return new McpToolSecurityResult(
                McpToolSecurityDecision.ALLOW,
                toolName,
                policyName,
                "");
    }

    public static McpToolSecurityResult deny(
            String toolName,
            String policyName,
            String reason) {

        return new McpToolSecurityResult(
                McpToolSecurityDecision.DENY,
                toolName,
                policyName,
                reason);
    }

    public boolean isAllowed() {

        return decision == McpToolSecurityDecision.ALLOW;
    }

    public boolean isDenied() {

        return decision == McpToolSecurityDecision.DENY;
    }
}