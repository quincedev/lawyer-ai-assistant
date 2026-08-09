package com.quince.lawyeraiassistant.agent.prompt.builder;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentTask;
import com.quince.lawyeraiassistant.agent.model.RuntimeReasonObservation;
import com.quince.lawyeraiassistant.agent.model.ToolObservation;
import com.quince.lawyeraiassistant.agent.prompt.model.RuntimeReasonPromptContext;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Runtime Reason Prompt Context Builder。
 */
@Component
public class RuntimeReasonPromptContextBuilder {

    public RuntimeReasonPromptContext build(
            AgentContext context,
            AgentTask task) {

        Objects.requireNonNull(
                context,
                "AgentContext must not be null");

        Objects.requireNonNull(
                task,
                "AgentTask must not be null");

        validateTaskBelongsToPlan(
                context,
                task);

        return new RuntimeReasonPromptContext(
                context.getGoal(),
                formatTask(
                        task),
                formatPlan(
                        context),
                formatObservations(
                        context));
    }

    private String formatTask(
            AgentTask task) {

        return "%s | %s | %s".formatted(
                task.getId(),
                task.getStatus(),
                task.getDescription());
    }

    private String formatPlan(
            AgentContext context) {

        if (context.getAgentPlan() == null
                || context.getAgentPlan()
                        .getTasks()
                        .isEmpty()) {

            return null;
        }

        return context.getAgentPlan()
                .getTasks()
                .stream()
                .map(
                        this::formatTask)
                .collect(
                        Collectors.joining(
                                "\n"));
    }

    private String formatObservations(
            AgentContext context) {

        String toolObservations = context.getObservations()
                .stream()
                .map(
                        this::formatObservation)
                .collect(
                        Collectors.joining(
                                "\n\n---\n\n"));

        String runtimeReasonObservations = context.getRuntimeReasonObservations()
                .stream()
                .map(
                        this::formatRuntimeReasonObservation)
                .collect(
                        Collectors.joining(
                                "\n\n---\n\n"));

        if (toolObservations.isBlank()
                && runtimeReasonObservations.isBlank()) {

            return null;
        }

        if (toolObservations.isBlank()) {
            return runtimeReasonObservations;
        }

        if (runtimeReasonObservations.isBlank()) {
            return toolObservations;
        }

        return toolObservations
                + "\n\n---\n\n"
                + runtimeReasonObservations;
    }

    private String formatRuntimeReasonObservation(
            RuntimeReasonObservation observation) {

        return """
                Type: REASON
                Task: %s
                Result:
                %s
                """.formatted(
                observation.getTaskId(),
                observation.getContent())
                .trim();
    }

    private String formatObservation(
            ToolObservation observation) {

        if (observation.isFailure()) {

            return """
                    Task: %s
                    Tool: %s
                    Status: FAILED
                    Error:
                    %s
                    """.formatted(
                    observation.getTaskId(),
                    observation.getToolName(),
                    observation.getErrorMessage())
                    .trim();
        }

        return """
                Task: %s
                Tool: %s
                Status: SUCCESS
                Result:
                %s
                """.formatted(
                observation.getTaskId(),
                observation.getToolName(),
                observation.getContent())
                .trim();
    }

    private void validateTaskBelongsToPlan(
            AgentContext context,
            AgentTask task) {

        if (context.getAgentPlan() == null) {
            return;
        }

        boolean exists = context.getAgentPlan()
                .getTasks()
                .stream()
                .anyMatch(
                        candidate -> candidate.getId()
                                .equals(
                                        task.getId()));

        if (!exists) {

            throw new IllegalArgumentException(
                    "AgentTask must belong to current AgentPlan");
        }
    }
}