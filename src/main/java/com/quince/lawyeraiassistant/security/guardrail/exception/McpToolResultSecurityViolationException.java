package com.quince.lawyeraiassistant.security.guardrail.exception;

import java.util.Objects;

import com.quince.lawyeraiassistant.security.mcp.result.McpToolResultSecurityResult;

/**
 * Internal exception representing an MCP Tool result
 * rejected by the result security boundary.
 */
public class McpToolResultSecurityViolationException
        extends RuntimeException {

    private static final String SAFE_MESSAGE = "MCP tool result rejected by security policy";

    private final McpToolResultSecurityResult result;

    public McpToolResultSecurityViolationException(
            McpToolResultSecurityResult result) {

        super(
                SAFE_MESSAGE);

        this.result = Objects.requireNonNull(
                result,
                "result must not be null");
    }

    public McpToolResultSecurityResult getResult() {

        return result;
    }
}