package com.quince.lawyeraiassistant.agent.tool.legal.mcp;

import com.quince.lawyeraiassistant.agent.tool.ToolExecutionContext;

import java.util.Map;

public interface LegalKnowledgeMcpClient {

    String search(
            ToolExecutionContext executionContext,
            Map<String, Object> arguments);
}