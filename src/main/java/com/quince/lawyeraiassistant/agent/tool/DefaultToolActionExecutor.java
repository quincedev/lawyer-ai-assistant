package com.quince.lawyeraiassistant.agent.tool;

import com.quince.lawyeraiassistant.agent.model.ToolAction;
import com.quince.lawyeraiassistant.agent.model.ToolExecutionResult;
import com.quince.lawyeraiassistant.agent.model.ToolObservation;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * ToolActionExecutor 默认实现。
 */
@Component
public class DefaultToolActionExecutor
        implements ToolActionExecutor {

    private final AgentToolRegistry toolRegistry;

    public DefaultToolActionExecutor(
            AgentToolRegistry toolRegistry) {

        this.toolRegistry = Objects.requireNonNull(
                toolRegistry,
                "toolRegistry must not be null");
    }

    @Override
    public ToolObservation execute(
            ToolAction action) {

        Objects.requireNonNull(
                action,
                "ToolAction must not be null");

        AgentTool tool = toolRegistry.get(
                action.getToolName());

        ToolExecutionResult result = tool.execute(
                action);

        Objects.requireNonNull(
                result,
                "ToolExecutionResult must not be null");

        if (result.isSuccess()) {

            return ToolObservation.success(
                    action.getTaskId(),
                    action.getToolName(),
                    result.getContent());
        }

        return ToolObservation.failure(
                action.getTaskId(),
                action.getToolName(),
                result.getErrorMessage());
    }
}