package com.quince.lawyeraiassistant.workflow.model;

import com.quince.lawyeraiassistant.workflow.node.WorkflowNode;
import com.quince.lawyeraiassistant.workflow.transition.WorkflowTransition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkflowDefinitionTest {

    @Test
    void shouldCreateWorkflowDefinition() {

        WorkflowNode prepare = WorkflowNode.of(
                "prepare-request",
                "Prepare Request",
                "准备请求");

        WorkflowNode analyze = WorkflowNode.of(
                "legal-analysis",
                "Legal Analysis",
                "执行法律分析");

        WorkflowTransition transition = WorkflowTransition.of(
                "prepare-request",
                "legal-analysis");

        WorkflowDefinition definition = WorkflowDefinition.of(
                "legal-workflow",
                "Legal Workflow",
                "prepare-request",
                List.of(
                        prepare,
                        analyze),
                List.of(
                        transition));

        assertEquals(
                "legal-workflow",
                definition.getId());

        assertEquals(
                "Legal Workflow",
                definition.getName());

        assertEquals(
                "prepare-request",
                definition.getStartNodeId());

        assertEquals(
                List.of(
                        prepare,
                        analyze),
                definition.getNodes());

        assertEquals(
                List.of(
                        transition),
                definition.getTransitions());
    }

    @Test
    void shouldReturnStartNode() {

        WorkflowNode prepare = WorkflowNode.of(
                "prepare-request",
                "Prepare Request",
                null);

        WorkflowNode analyze = WorkflowNode.of(
                "legal-analysis",
                "Legal Analysis",
                null);

        WorkflowDefinition definition = WorkflowDefinition.of(
                "legal-workflow",
                "Legal Workflow",
                "prepare-request",
                List.of(
                        prepare,
                        analyze),
                List.of());

        assertSame(
                prepare,
                definition.getStartNode());
    }

    @Test
    void shouldFindNodeById() {

        WorkflowNode prepare = WorkflowNode.of(
                "prepare-request",
                "Prepare Request",
                null);

        WorkflowNode analyze = WorkflowNode.of(
                "legal-analysis",
                "Legal Analysis",
                null);

        WorkflowDefinition definition = WorkflowDefinition.of(
                "legal-workflow",
                "Legal Workflow",
                "prepare-request",
                List.of(
                        prepare,
                        analyze),
                List.of());

        WorkflowNode result = definition.findNode(
                "legal-analysis");

        assertSame(
                analyze,
                result);
    }

    @Test
    void shouldRejectUnknownNode() {

        WorkflowDefinition definition = createDefinition();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> definition.findNode(
                        "unknown-node"));

        assertEquals(
                "Workflow node not found: unknown-node",
                exception.getMessage());
    }

    @Test
    void shouldRejectNullNodeIdWhenFindingNode() {

        WorkflowDefinition definition = createDefinition();

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> definition.findNode(
                        null));

        assertEquals(
                "Node id must not be null",
                exception.getMessage());
    }

    @Test
    void shouldRejectEmptyNodes() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> WorkflowDefinition.of(
                        "legal-workflow",
                        "Legal Workflow",
                        "prepare-request",
                        List.of(),
                        List.of()));

        assertEquals(
                "Workflow nodes must not be empty",
                exception.getMessage());
    }

    @Test
    void shouldRejectMissingStartNode() {

        WorkflowNode analyze = WorkflowNode.of(
                "legal-analysis",
                "Legal Analysis",
                null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> WorkflowDefinition.of(
                        "legal-workflow",
                        "Legal Workflow",
                        "prepare-request",
                        List.of(
                                analyze),
                        List.of()));

        assertEquals(
                "Start node does not exist: prepare-request",
                exception.getMessage());
    }

    @Test
    void shouldRejectNullNodes() {

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> WorkflowDefinition.of(
                        "legal-workflow",
                        "Legal Workflow",
                        "prepare-request",
                        null,
                        List.of()));

        assertEquals(
                "Workflow nodes must not be null",
                exception.getMessage());
    }

    @Test
    void shouldRejectNullTransitions() {

        WorkflowNode prepare = WorkflowNode.of(
                "prepare-request",
                "Prepare Request",
                null);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> WorkflowDefinition.of(
                        "legal-workflow",
                        "Legal Workflow",
                        "prepare-request",
                        List.of(
                                prepare),
                        null));

        assertEquals(
                "Workflow transitions must not be null",
                exception.getMessage());
    }

    @Test
    void shouldRejectNullWorkflowId() {

        WorkflowNode prepare = WorkflowNode.of(
                "prepare-request",
                "Prepare Request",
                null);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> WorkflowDefinition.of(
                        null,
                        "Legal Workflow",
                        "prepare-request",
                        List.of(
                                prepare),
                        List.of()));

        assertEquals(
                "Workflow id must not be blank",
                exception.getMessage());
    }

    @Test
    void shouldRejectBlankWorkflowName() {

        WorkflowNode prepare = WorkflowNode.of(
                "prepare-request",
                "Prepare Request",
                null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> WorkflowDefinition.of(
                        "legal-workflow",
                        "   ",
                        "prepare-request",
                        List.of(
                                prepare),
                        List.of()));

        assertEquals(
                "Workflow name must not be blank",
                exception.getMessage());
    }

    @Test
    void shouldRejectTransitionWithUnknownFromNode() {

        WorkflowNode nodeA = WorkflowNode.of(
                "node-a",
                "Node A",
                null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> WorkflowDefinition.of(
                        "workflow",
                        "Workflow",
                        "node-a",
                        List.of(
                                nodeA),
                        List.of(
                                WorkflowTransition.of(
                                        "unknown-node",
                                        "node-a"))));

        assertEquals(
                "Transition from node does not exist: unknown-node",
                exception.getMessage());
    }

    @Test
    void shouldRejectTransitionWithUnknownToNode() {

        WorkflowNode nodeA = WorkflowNode.of(
                "node-a",
                "Node A",
                null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> WorkflowDefinition.of(
                        "workflow",
                        "Workflow",
                        "node-a",
                        List.of(
                                nodeA),
                        List.of(
                                WorkflowTransition.of(
                                        "node-a",
                                        "unknown-node"))));

        assertEquals(
                "Transition to node does not exist: unknown-node",
                exception.getMessage());
    }

    private WorkflowDefinition createDefinition() {

        WorkflowNode prepare = WorkflowNode.of(
                "prepare-request",
                "Prepare Request",
                null);

        WorkflowNode analyze = WorkflowNode.of(
                "legal-analysis",
                "Legal Analysis",
                null);

        return WorkflowDefinition.of(
                "legal-workflow",
                "Legal Workflow",
                "prepare-request",
                List.of(
                        prepare,
                        analyze),
                List.of(
                        WorkflowTransition.of(
                                "prepare-request",
                                "legal-analysis")));
    }
}