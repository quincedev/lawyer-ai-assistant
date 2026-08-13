package com.quince.lawyeraiassistant.workflow.transition;

import com.quince.lawyeraiassistant.workflow.condition.WorkflowCondition;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

/**
 * Workflow Node 之间的状态流转关系。
 */
@Getter
@ToString
@EqualsAndHashCode
public final class WorkflowTransition {

    private static final WorkflowCondition ALWAYS = context -> true;

    private final String fromNodeId;

    private final String toNodeId;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private final WorkflowCondition condition;

    private WorkflowTransition(
            String fromNodeId,
            String toNodeId,
            WorkflowCondition condition) {

        this.fromNodeId = requireText(
                fromNodeId,
                "From node id must not be blank");

        this.toNodeId = requireText(
                toNodeId,
                "To node id must not be blank");

        this.condition = Objects.requireNonNull(
                condition,
                "WorkflowCondition must not be null");
    }

    /**
     * 无条件 Transition。
     */
    public static WorkflowTransition of(
            String fromNodeId,
            String toNodeId) {

        return new WorkflowTransition(
                fromNodeId,
                toNodeId,
                ALWAYS);
    }

    /**
     * 条件 Transition。
     */
    public static WorkflowTransition when(
            String fromNodeId,
            String toNodeId,
            WorkflowCondition condition) {

        return new WorkflowTransition(
                fromNodeId,
                toNodeId,
                condition);
    }

    public boolean matches(
            com.quince.lawyeraiassistant.workflow.model.WorkflowContext context) {

        return condition.matches(
                context);
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
}