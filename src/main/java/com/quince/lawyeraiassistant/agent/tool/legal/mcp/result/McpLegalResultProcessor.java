package com.quince.lawyeraiassistant.agent.tool.legal.mcp.result;

import com.quince.lawyeraiassistant.agent.model.ToolExecutionResult;

public interface McpLegalResultProcessor {

    ToolExecutionResult process(
            String rawResult);
}