package com.quince.lawyeraiassistant.workflow.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 某一次 Workflow 执行过程中的 Runtime Context。
 */
@Getter
@ToString
@EqualsAndHashCode
public final class WorkflowContext {

    private final String workflowId;

    private final String currentNodeId;

    private final WorkflowStatus status;

    private final Map<String, Object> variables;

    private final Map<String, WorkflowNodeStatus> nodeStatuses;

    private final String errorMessage;

    @Builder(toBuilder = true)
    private WorkflowContext(
            String workflowId,
            String currentNodeId,
            WorkflowStatus status,
            Map<String, Object> variables,
            Map<String, WorkflowNodeStatus> nodeStatuses,
            String errorMessage) {

        this.workflowId = requireText(
                workflowId,
                "Workflow id must not be blank");

        this.currentNodeId = normalize(
                currentNodeId);

        this.status = Objects.requireNonNull(
                status,
                "Workflow status must not be null");

        this.variables = Map.copyOf(
                variables == null
                        ? Map.of()
                        : new LinkedHashMap<>(
                                variables));

        this.nodeStatuses = Map.copyOf(
                nodeStatuses == null
                        ? Map.of()
                        : new LinkedHashMap<>(
                                nodeStatuses));
        this.errorMessage = normalize(errorMessage);
    }

    public static WorkflowContext pending(
            WorkflowDefinition definition) {

        Objects.requireNonNull(
                definition,
                "WorkflowDefinition must not be null");

        Map<String, WorkflowNodeStatus> statuses = new LinkedHashMap<>();

        definition.getNodes()
                .forEach(
                        node -> statuses.put(
                                node.getId(),
                                WorkflowNodeStatus.PENDING));

        return WorkflowContext.builder()
                .workflowId(
                        definition.getId())
                .currentNodeId(
                        definition.getStartNodeId())
                .status(
                        WorkflowStatus.PENDING)
                .variables(
                        Map.of())
                .nodeStatuses(
                        statuses)
                .errorMessage("")
                .build();
    }

    public Object getVariable(
            String name) {

        Objects.requireNonNull(
                name,
                "Variable name must not be null");

        return variables.get(
                name);
    }

    public WorkflowNodeStatus getNodeStatus(
            String nodeId) {

        Objects.requireNonNull(
                nodeId,
                "Node id must not be null");

        return nodeStatuses.get(
                nodeId);
    }

    public WorkflowContext withStatus(
            WorkflowStatus status) {

        Objects.requireNonNull(
                status,
                "Workflow status must not be null");

        return toBuilder()
                .status(status)
                .build();
    }

    public WorkflowContext withCurrentNode(
            String nodeId) {

        return toBuilder()
                .currentNodeId(
                        requireText(
                                nodeId,
                                "Current node id must not be blank"))
                .build();
    }

    public WorkflowContext withNodeStatus(
            String nodeId,
            WorkflowNodeStatus status) {

        Objects.requireNonNull(
                nodeId,
                "Node id must not be null");

        Objects.requireNonNull(
                status,
                "Node status must not be null");

        Map<String, WorkflowNodeStatus> updated = new LinkedHashMap<>(
                nodeStatuses);

        updated.put(
                nodeId,
                status);

        return toBuilder()
                .nodeStatuses(
                        updated)
                .build();
    }

    public WorkflowContext mergeVariables(
            Map<String, Object> newVariables) {

        if (newVariables == null
                || newVariables.isEmpty()) {

            return this;
        }

        Map<String, Object> updated = new LinkedHashMap<>(
                variables);

        updated.putAll(
                newVariables);

        return toBuilder()
                .variables(
                        updated)
                .build();
    }

    public WorkflowContext fail(
            String errorMessage) {

        Objects.requireNonNull(
                errorMessage,
                "Error message must not be null");

        if (errorMessage.isBlank()) {
            throw new IllegalArgumentException(
                    "Error message must not be blank");
        }

        return toBuilder()
                .status(
                        WorkflowStatus.FAILED)
                .errorMessage(
                        errorMessage.trim())
                .build();
    }

    private static String requireText(
            String value,
            String message) {

        Objects.requireNonNull(
                value,
                message);

        String normalized = value.trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    message);
        }

        return normalized;
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