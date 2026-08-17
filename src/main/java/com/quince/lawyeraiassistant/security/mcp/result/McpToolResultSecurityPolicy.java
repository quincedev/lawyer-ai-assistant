package com.quince.lawyeraiassistant.security.mcp.result;

/**
 * Security policy applied to data returned by an MCP Tool
 * before the data is exposed to the Agent / LLM context.
 */
public interface McpToolResultSecurityPolicy {

    /**
     * Stable policy name used for diagnostics and audit.
     */
    String name();

    /**
     * Evaluates data returned by an MCP Tool.
     *
     * @param toolName MCP Tool that produced the result
     * @param content  textual Tool result
     * @return security decision
     */
    McpToolResultSecurityResult evaluate(
            String toolName,
            String content);
}