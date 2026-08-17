package com.quince.lawyeraiassistant.security.mcp;

import java.util.Map;

/**
 * Security policy applied to an incoming MCP Tool invocation.
 *
 * <p>
 * Policies are evaluated on the MCP Server side before the
 * underlying Tool capability is executed.
 * </p>
 */
public interface McpToolSecurityPolicy {

    /**
     * Stable policy name used for diagnostics and audit.
     */
    String name();

    /**
     * Evaluates whether an MCP Tool invocation may continue.
     *
     * @param toolName  requested MCP Tool
     * @param arguments invocation arguments
     * @return security decision
     */
    McpToolSecurityResult evaluate(
            String toolName,
            Map<String, Object> arguments);
}