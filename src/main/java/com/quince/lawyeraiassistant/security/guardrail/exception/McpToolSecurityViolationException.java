package com.quince.lawyeraiassistant.security.guardrail.exception;

import java.util.Objects;

import com.quince.lawyeraiassistant.security.mcp.McpToolSecurityResult;

/**
 * Internal exception representing an MCP request security rejection.
 *
 * The detailed security result is retained internally,
 * while the exception message remains safe for transport.
 */
public class McpToolSecurityViolationException
        extends RuntimeException {

    private static final String SAFE_MESSAGE = "MCP tool request rejected by security policy";

    private final McpToolSecurityResult result;

    public McpToolSecurityViolationException(
            McpToolSecurityResult result) {

        super(
                SAFE_MESSAGE);

        this.result = Objects.requireNonNull(
                result,
                "result must not be null");
    }

    public McpToolSecurityResult getResult() {

        return result;
    }
}