package com.quince.lawyeraiassistant.security.mcp.result;

/**
 * Security decision for data returned by an MCP Tool.
 */
public enum McpToolResultSecurityDecision {

    /**
     * The MCP Tool result may enter the Agent context.
     */
    ALLOW,

    /**
     * The MCP Tool result must not enter the Agent context.
     */
    DENY
}