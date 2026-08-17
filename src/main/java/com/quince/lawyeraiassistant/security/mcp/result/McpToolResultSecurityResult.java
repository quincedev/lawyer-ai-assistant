package com.quince.lawyeraiassistant.security.mcp.result;

import java.util.Objects;

/**
 * Immutable security result produced when evaluating
 * data returned by an MCP Tool.
 *
 * @param decision   security decision
 * @param toolName   MCP Tool that produced the result
 * @param policyName policy that produced the decision
 * @param reason     optional explanation for the decision
 */
public record McpToolResultSecurityResult(
        McpToolResultSecurityDecision decision,
        String toolName,
        String policyName,
        String reason) {

    public McpToolResultSecurityResult {

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

    public static McpToolResultSecurityResult allow(
            String toolName,
            String policyName) {

        return new McpToolResultSecurityResult(
                McpToolResultSecurityDecision.ALLOW,
                toolName,
                policyName,
                "");
    }

    public static McpToolResultSecurityResult deny(
            String toolName,
            String policyName,
            String reason) {

        return new McpToolResultSecurityResult(
                McpToolResultSecurityDecision.DENY,
                toolName,
                policyName,
                reason);
    }

    public boolean isAllowed() {

        return decision == McpToolResultSecurityDecision.ALLOW;
    }

    public boolean isDenied() {

        return decision == McpToolResultSecurityDecision.DENY;
    }
}