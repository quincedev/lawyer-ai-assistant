package com.quince.lawyeraiassistant.agent.operator;

import com.quince.lawyeraiassistant.agent.model.AgentAction;
import com.quince.lawyeraiassistant.agent.model.AgentActionExecutionResult;
import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentTask;
import com.quince.lawyeraiassistant.agent.model.ToolObservation;
import com.quince.lawyeraiassistant.agent.service.AgentFinalAnswerService;
import com.quince.lawyeraiassistant.agent.service.AgentRuntimeReasonService;
import com.quince.lawyeraiassistant.agent.tool.ToolActionExecutor;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * AgentActionExecutionOperator 默认实现。
 *
 * <p>
 * 根据 AgentActionType 将 Action 分派到对应执行能力。
 * </p>
 */
@Component
public class DefaultAgentActionExecutionOperator
        implements AgentActionExecutionOperator {

    private final ToolActionExecutor toolActionExecutor;

    private final AgentRuntimeReasonService runtimeReasonService;

    private final AgentFinalAnswerService finalAnswerService;

    public DefaultAgentActionExecutionOperator(
            ToolActionExecutor toolActionExecutor,
            AgentRuntimeReasonService runtimeReasonService,
            AgentFinalAnswerService finalAnswerService) {

        this.toolActionExecutor = Objects.requireNonNull(
                toolActionExecutor,
                "ToolActionExecutor must not be null");

        this.runtimeReasonService = Objects.requireNonNull(
                runtimeReasonService,
                "AgentRuntimeReasonService must not be null");

        this.finalAnswerService = Objects.requireNonNull(
                finalAnswerService,
                "AgentFinalAnswerService must not be null");
    }

    @Override
    public AgentActionExecutionResult execute(
            AgentContext context,
            AgentTask task,
            AgentAction action) {

        Objects.requireNonNull(
                context,
                "AgentContext must not be null");

        Objects.requireNonNull(
                task,
                "AgentTask must not be null");

        Objects.requireNonNull(
                action,
                "AgentAction must not be null");

        validateTaskMatch(
                task,
                action);

        return switch (action.getType()) {

            case TOOL ->
                executeTool(
                        action);

            case REASON ->
                executeReason(
                        context,
                        task);

            case FINAL_ANSWER ->
                executeFinalAnswer(
                        context);
        };
    }

    private AgentActionExecutionResult executeTool(
            AgentAction action) {

        ToolObservation observation = toolActionExecutor.execute(
                action.requireToolAction());

        return AgentActionExecutionResult.tool(
                observation);
    }

    private AgentActionExecutionResult executeReason(
            AgentContext context,
            AgentTask task) {

        String reason = runtimeReasonService.reason(
                context,
                task);

        return AgentActionExecutionResult.reason(
                reason);
    }

    private AgentActionExecutionResult executeFinalAnswer(
            AgentContext context) {

        String finalAnswer = finalAnswerService.generate(
                context);

        return AgentActionExecutionResult.finalAnswer(
                finalAnswer);
    }

    private void validateTaskMatch(
            AgentTask task,
            AgentAction action) {

        if (!task.getId()
                .equals(
                        action.getTaskId())) {

            throw new IllegalArgumentException(
                    "AgentAction taskId must match AgentTask id");
        }
    }
}