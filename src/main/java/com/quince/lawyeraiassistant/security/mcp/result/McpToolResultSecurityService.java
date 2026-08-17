package com.quince.lawyeraiassistant.security.mcp.result;

/**
 * Security boundary for data returned by MCP Tools
 * before it enters the Agent / LLM context.
 */
public interface McpToolResultSecurityService {

    McpToolResultSecurityResult evaluate(
            String toolName,
            String content);
}