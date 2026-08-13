package com.quince.lawyeraiassistant.workflow.model;

import com.quince.lawyeraiassistant.workflow.node.WorkflowNode;
import com.quince.lawyeraiassistant.workflow.transition.WorkflowTransition;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.Objects;

/**
 * Workflow 的静态定义。
 *
 * <p>
 * 描述 Workflow：
 * </p>
 *
 * <ul>
 * <li>有哪些 Node</li>
 * <li>从哪个 Node 开始</li>
 * <li>Node 之间如何 Transition</li>
 * </ul>
 */
@Getter
@ToString
@EqualsAndHashCode
public final class WorkflowDefinition {

    private final String id;

    private final String name;

    private final String startNodeId;

    private final List<WorkflowNode> nodes;

    private final List<WorkflowTransition> transitions;

    @Builder
    private WorkflowDefinition(
            String id,
            String name,
            String startNodeId,
            List<WorkflowNode> nodes,
            List<WorkflowTransition> transitions) {

        this.id = requireText(
                id,
                "Workflow id must not be blank");

        this.name = requireText(
                name,
                "Workflow name must not be blank");

        this.startNodeId = requireText(
                startNodeId,
                "Start node id must not be blank");

        this.nodes = List.copyOf(
                Objects.requireNonNull(
                        nodes,
                        "Workflow nodes must not be null"));

        this.transitions = List.copyOf(
                Objects.requireNonNull(
                        transitions,
                        "Workflow transitions must not be null"));

        validate();
    }

    public static WorkflowDefinition of(
            String id,
            String name,
            String startNodeId,
            List<WorkflowNode> nodes,
            List<WorkflowTransition> transitions) {

        return WorkflowDefinition.builder()
                .id(id)
                .name(name)
                .startNodeId(startNodeId)
                .nodes(nodes)
                .transitions(transitions)
                .build();
    }

    public WorkflowNode getStartNode() {

        return findNode(
                startNodeId);
    }

    public WorkflowNode findNode(
            String nodeId) {

        Objects.requireNonNull(
                nodeId,
                "Node id must not be null");

        return nodes.stream()
                .filter(
                        node -> node.getId()
                                .equals(
                                        nodeId))
                .findFirst()
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Workflow node not found: "
                                        + nodeId));
    }

    private void validate() {

        if (nodes.isEmpty()) {
            throw new IllegalArgumentException(
                    "Workflow nodes must not be empty");
        }

        boolean startNodeExists = nodes.stream()
                .anyMatch(
                        node -> node.getId()
                                .equals(
                                        startNodeId));

        if (!startNodeExists) {
            throw new IllegalArgumentException(
                    "Start node does not exist: "
                            + startNodeId);
        }

        validateTransitions();
    }

    private void validateTransitions() {

        for (WorkflowTransition transition : transitions) {

            boolean fromExists = nodes.stream()
                    .anyMatch(
                            node -> node.getId()
                                    .equals(
                                            transition.getFromNodeId()));

            if (!fromExists) {
                throw new IllegalArgumentException(
                        "Transition from node does not exist: "
                                + transition.getFromNodeId());
            }

            boolean toExists = nodes.stream()
                    .anyMatch(
                            node -> node.getId()
                                    .equals(
                                            transition.getToNodeId()));

            if (!toExists) {
                throw new IllegalArgumentException(
                        "Transition to node does not exist: "
                                + transition.getToNodeId());
            }
        }
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