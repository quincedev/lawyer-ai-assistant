package com.quince.lawyeraiassistant.agent.action.policy;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentTask;
import com.quince.lawyeraiassistant.agent.model.ToolObservation;

@Component
public class DuplicateToolCallPolicy {

    private static final List<String> ANALYTICAL_KEYWORDS = List.of(
            "分析",
            "归纳",
            "总结",
            "梳理",
            "比较",
            "区分",
            "推理",
            "形成结论",
            "综合");

    public boolean shouldBlock(
            AgentContext context,
            AgentTask currentTask,
            String toolName) {

        Objects.requireNonNull(
                context,
                "AgentContext must not be null");

        Objects.requireNonNull(
                currentTask,
                "AgentTask must not be null");

        if (toolName == null
                || toolName.isBlank()) {

            return false;
        }

        return hasDeterministicFailure(
                context,
                currentTask,
                toolName)
                || isAnalyticalTask(
                        currentTask)
                        && hasPreviousSuccessfulToolCall(
                                context,
                                toolName);
    }

    private boolean hasPreviousSuccessfulToolCall(
            AgentContext context,
            String toolName) {

        return context.getObservations()
                .stream()
                .filter(
                        observation -> toolName.equals(
                                observation.getToolName()))
                .anyMatch(
                        observation -> !observation.isFailure());
    }

    private boolean hasDeterministicFailure(
            AgentContext context,
            AgentTask task,
            String toolName) {

        return context.getObservations()
                .stream()
                .filter(
                        observation -> task.getId()
                                .equals(
                                        observation.getTaskId()))
                .filter(
                        observation -> toolName.equals(
                                observation.getToolName()))
                .filter(
                        ToolObservation::isFailure)
                .anyMatch(
                        this::isDeterministicFailure);
    }

    private boolean isDeterministicFailure(
            ToolObservation observation) {

        String errorMessage = observation.getErrorMessage();

        if (errorMessage == null) {

            return false;
        }

        return errorMessage.contains(
                "Maximum Observation length exceeded")
                || errorMessage.contains(
                        "Maximum Agent context length exceeded");
    }

    private boolean isAnalyticalTask(
            AgentTask task) {

        String description = task.getDescription() == null
                ? ""
                : task.getDescription()
                        .toLowerCase(
                                Locale.ROOT);

        return ANALYTICAL_KEYWORDS
                .stream()
                .anyMatch(
                        description::contains);
    }
}