package com.quince.lawyeraiassistant.security.mcp;

import java.util.Map;

/**
 * Server-side security boundary for incoming MCP Tool invocations.
 */
public interface McpToolSecurityService {

    /**
     * Evaluates all configured MCP Tool security policies.
     *
     * @param toolName  requested MCP Tool
     * @param arguments invocation arguments
     * @return final security result
     */
    McpToolSecurityResult evaluate(
            String toolName,
            Map<String, Object> arguments);
}