package com.quince.lawyeraiassistant.security.mcp;

/**
 * Security decision for an incoming MCP Tool invocation.
 */
public enum McpToolSecurityDecision {

    /**
     * The MCP Tool invocation may continue.
     */
    ALLOW,

    /**
     * The MCP Tool invocation must be rejected.
     */
    DENY
}