package com.quince.lawyeraiassistant.agent.tool;

import com.quince.lawyeraiassistant.agent.model.ToolAction;
import com.quince.lawyeraiassistant.agent.model.ToolExecutionResult;
import com.quince.lawyeraiassistant.security.legal.SecuritySource;

public interface AgentTool {

    String name();

    /**
     * Legacy/internal execution path.
     */
    ToolExecutionResult execute(
            ToolAction action);

    /**
     * Trusted runtime execution path.
     *
     * <p>
     * Tenant identity must be supplied through ToolExecutionContext,
     * never through LLM-controlled ToolAction arguments.
     * </p>
     */
    default ToolExecutionResult execute(
            ToolExecutionContext executionContext,
            ToolAction action) {

        return execute(
                action);
    }

    default SecuritySource resultSecuritySource() {

        return SecuritySource.TOOL_RESULT;
    }
}