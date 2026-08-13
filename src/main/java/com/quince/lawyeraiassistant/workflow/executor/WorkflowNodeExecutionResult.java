package com.quince.lawyeraiassistant.workflow.executor;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Map;
import java.util.Objects;

/**
 * Workflow Node 的执行结果。
 */
@Getter
@ToString
@EqualsAndHashCode
public final class WorkflowNodeExecutionResult {

    private final boolean success;

    private final Map<String, Object> variables;

    private final String errorMessage;

    private WorkflowNodeExecutionResult(
            boolean success,
            Map<String, Object> variables,
            String errorMessage) {

        this.success = success;

        this.variables = Map.copyOf(
                variables == null
                        ? Map.of()
                        : variables);

        this.errorMessage = normalize(
                errorMessage);
    }

    public static WorkflowNodeExecutionResult success() {

        return new WorkflowNodeExecutionResult(
                true,
                Map.of(),
                "");
    }

    public static WorkflowNodeExecutionResult success(
            Map<String, Object> variables) {

        return new WorkflowNodeExecutionResult(
                true,
                variables,
                "");
    }

    public static WorkflowNodeExecutionResult failure(
            String errorMessage) {

        Objects.requireNonNull(
                errorMessage,
                "Error message must not be null");

        if (errorMessage.isBlank()) {
            throw new IllegalArgumentException(
                    "Error message must not be blank");
        }

        return new WorkflowNodeExecutionResult(
                false,
                Map.of(),
                errorMessage.trim());
    }

    private static String normalize(
            String value) {

        if (value == null
                || value.isBlank()) {

            return "";
        }

        return value.trim();
    }
}