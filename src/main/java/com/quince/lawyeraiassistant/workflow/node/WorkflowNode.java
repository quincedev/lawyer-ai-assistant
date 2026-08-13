package com.quince.lawyeraiassistant.workflow.node;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

/**
 * Workflow 中的一个业务执行节点。
 *
 * <p>
 * 当前对象只负责描述 Workflow Node，
 * 不直接负责节点执行逻辑。
 * </p>
 */
@Getter
@ToString
@EqualsAndHashCode
public final class WorkflowNode {

    private final String id;

    private final String name;

    private final String description;

    private final WorkflowNodeType type;

    private WorkflowNode(
            String id,
            String name,
            String description,
            WorkflowNodeType type) {

        this.id = requireText(
                id,
                "Node id must not be blank");

        this.name = requireText(
                name,
                "Node name must not be blank");

        this.description = normalize(
                description);

        this.type = Objects.requireNonNull(
                type,
                "WorkflowNodeType must not be null");
    }

    public static WorkflowNode of(
            String id,
            String name,
            String description) {

        return new WorkflowNode(
                id,
                name,
                description,
                WorkflowNodeType.STANDARD);
    }

    public static WorkflowNode agent(
            String id,
            String name,
            String description) {

        return new WorkflowNode(
                id,
                name,
                description,
                WorkflowNodeType.AGENT);
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