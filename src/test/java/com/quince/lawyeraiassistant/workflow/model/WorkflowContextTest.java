package com.quince.lawyeraiassistant.workflow.model;

import com.quince.lawyeraiassistant.workflow.node.WorkflowNode;
import com.quince.lawyeraiassistant.workflow.transition.WorkflowTransition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkflowContextTest {

    @Test
    void shouldCreatePendingWorkflowContext() {

        WorkflowDefinition definition = createDefinition();

        WorkflowContext context = WorkflowContext.pending(
                definition);

        assertEquals(
                "legal-analysis-workflow",
                context.getWorkflowId());

        assertEquals(
                "prepare-request",
                context.getCurrentNodeId());

        assertEquals(
                WorkflowStatus.PENDING,
                context.getStatus());

        assertEquals(
                3,
                context.getNodeStatuses()
                        .size());

        assertEquals(
                WorkflowNodeStatus.PENDING,
                context.getNodeStatus(
                        "prepare-request"));

        assertEquals(
                WorkflowNodeStatus.PENDING,
                context.getNodeStatus(
                        "legal-analysis"));

        assertEquals(
                WorkflowNodeStatus.PENDING,
                context.getNodeStatus(
                        "generate-result"));

        assertEquals(
                0,
                context.getVariables()
                        .size());
    }

    @Test
    void shouldInitializeAllNodesAsPending() {

        WorkflowContext context = WorkflowContext.pending(
                createDefinition());

        context.getNodeStatuses()
                .values()
                .forEach(
                        status -> assertEquals(
                                WorkflowNodeStatus.PENDING,
                                status));
    }

    @Test
    void shouldReturnVariable() {

        WorkflowContext context = WorkflowContext.builder()
                .workflowId(
                        "legal-workflow")
                .currentNodeId(
                        "prepare-request")
                .status(
                        WorkflowStatus.RUNNING)
                .variables(
                        java.util.Map.of(
                                "goal",
                                "分析劳动合同"))
                .nodeStatuses(
                        java.util.Map.of())
                .build();

        assertEquals(
                "分析劳动合同",
                context.getVariable(
                        "goal"));
    }

    @Test
    void shouldReturnNullForMissingVariable() {

        WorkflowContext context = WorkflowContext.pending(
                createDefinition());

        assertNull(
                context.getVariable(
                        "missing"));
    }

    @Test
    void shouldReturnNullForUnknownNodeStatus() {

        WorkflowContext context = WorkflowContext.pending(
                createDefinition());

        assertNull(
                context.getNodeStatus(
                        "unknown-node"));
    }

    @Test
    void shouldRejectNullWorkflowDefinition() {

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> WorkflowContext.pending(
                        null));

        assertEquals(
                "WorkflowDefinition must not be null",
                exception.getMessage());
    }

    @Test
    void shouldRejectNullVariableName() {

        WorkflowContext context = WorkflowContext.pending(
                createDefinition());

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> context.getVariable(
                        null));

        assertEquals(
                "Variable name must not be null",
                exception.getMessage());
    }

    @Test
    void shouldRejectNullNodeIdWhenGettingStatus() {

        WorkflowContext context = WorkflowContext.pending(
                createDefinition());

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> context.getNodeStatus(
                        null));

        assertEquals(
                "Node id must not be null",
                exception.getMessage());
    }

    @Test
    void shouldDefaultVariablesToEmptyMap() {

        WorkflowContext context = WorkflowContext.builder()
                .workflowId(
                        "legal-workflow")
                .currentNodeId(
                        "prepare-request")
                .status(
                        WorkflowStatus.PENDING)
                .variables(
                        null)
                .nodeStatuses(
                        null)
                .build();

        assertEquals(
                0,
                context.getVariables()
                        .size());

        assertEquals(
                0,
                context.getNodeStatuses()
                        .size());
    }

    @Test
    void shouldRejectNullWorkflowStatus() {

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> WorkflowContext.builder()
                        .workflowId(
                                "legal-workflow")
                        .currentNodeId(
                                "prepare-request")
                        .status(
                                null)
                        .build());

        assertEquals(
                "Workflow status must not be null",
                exception.getMessage());
    }

    @Test
    void shouldUpdateWorkflowStatus() {

        WorkflowContext context = WorkflowContext.pending(
                createDefinition());

        WorkflowContext updated = context.withStatus(
                WorkflowStatus.RUNNING);

        assertEquals(
                WorkflowStatus.PENDING,
                context.getStatus());

        assertEquals(
                WorkflowStatus.RUNNING,
                updated.getStatus());
    }

    @Test
    void shouldUpdateCurrentNode() {

        WorkflowContext context = WorkflowContext.pending(
                createDefinition());

        WorkflowContext updated = context.withCurrentNode(
                "legal-analysis");

        assertEquals(
                "prepare-request",
                context.getCurrentNodeId());

        assertEquals(
                "legal-analysis",
                updated.getCurrentNodeId());
    }

    @Test
    void shouldUpdateNodeStatus() {

        WorkflowContext context = WorkflowContext.pending(
                createDefinition());

        WorkflowContext updated = context.withNodeStatus(
                "prepare-request",
                WorkflowNodeStatus.RUNNING);

        assertEquals(
                WorkflowNodeStatus.PENDING,
                context.getNodeStatus(
                        "prepare-request"));

        assertEquals(
                WorkflowNodeStatus.RUNNING,
                updated.getNodeStatus(
                        "prepare-request"));
    }

    @Test
    void shouldMergeVariables() {

        WorkflowContext context = WorkflowContext.builder()
                .workflowId(
                        "legal-workflow")
                .currentNodeId(
                        "prepare-request")
                .status(
                        WorkflowStatus.RUNNING)
                .variables(
                        java.util.Map.of(
                                "goal",
                                "分析合同"))
                .nodeStatuses(
                        java.util.Map.of())
                .build();

        WorkflowContext updated = context.mergeVariables(
                java.util.Map.of(
                        "riskLevel",
                        "HIGH"));

        assertEquals(
                "分析合同",
                updated.getVariable(
                        "goal"));

        assertEquals(
                "HIGH",
                updated.getVariable(
                        "riskLevel"));

        assertNull(
                context.getVariable(
                        "riskLevel"));
    }

    @Test
    void shouldOverrideExistingVariableWhenMerging() {

        WorkflowContext context = WorkflowContext.builder()
                .workflowId(
                        "legal-workflow")
                .currentNodeId(
                        "prepare-request")
                .status(
                        WorkflowStatus.RUNNING)
                .variables(
                        java.util.Map.of(
                                "riskLevel",
                                "LOW"))
                .nodeStatuses(
                        java.util.Map.of())
                .build();

        WorkflowContext updated = context.mergeVariables(
                java.util.Map.of(
                        "riskLevel",
                        "HIGH"));

        assertEquals(
                "HIGH",
                updated.getVariable(
                        "riskLevel"));
    }

    @Test
    void shouldReturnSameContextWhenMergingEmptyVariables() {

        WorkflowContext context = WorkflowContext.pending(
                createDefinition());

        WorkflowContext result = context.mergeVariables(
                java.util.Map.of());

        assertSame(
                context,
                result);
    }

    @Test
    void shouldReturnSameContextWhenMergingNullVariables() {

        WorkflowContext context = WorkflowContext.pending(
                createDefinition());

        WorkflowContext result = context.mergeVariables(
                null);

        assertSame(
                context,
                result);
    }

    @Test
    void shouldRejectNullStatusWhenUpdatingWorkflowStatus() {

        WorkflowContext context = WorkflowContext.pending(
                createDefinition());

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> context.withStatus(
                        null));

        assertEquals(
                "Workflow status must not be null",
                exception.getMessage());
    }

    @Test
    void shouldRejectBlankCurrentNode() {

        WorkflowContext context = WorkflowContext.pending(
                createDefinition());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> context.withCurrentNode(
                        "   "));

        assertEquals(
                "Current node id must not be blank",
                exception.getMessage());
    }

    @Test
    void shouldRejectNullNodeStatus() {

        WorkflowContext context = WorkflowContext.pending(
                createDefinition());

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> context.withNodeStatus(
                        "prepare-request",
                        null));

        assertEquals(
                "Node status must not be null",
                exception.getMessage());
    }

    @Test
    void shouldFailWorkflowWithErrorMessage() {

        WorkflowContext context = WorkflowContext.pending(
                createDefinition());

        WorkflowContext failed = context.fail(
                "Node execution failed");

        assertEquals(
                WorkflowStatus.FAILED,
                failed.getStatus());

        assertEquals(
                "Node execution failed",
                failed.getErrorMessage());
    }

    @Test
    void shouldNormalizeFailureMessage() {

        WorkflowContext context = WorkflowContext.pending(
                createDefinition());

        WorkflowContext failed = context.fail(
                "  Node execution failed  ");

        assertEquals(
                "Node execution failed",
                failed.getErrorMessage());
    }

    @Test
    void shouldRejectNullFailureMessage() {

        WorkflowContext context = WorkflowContext.pending(
                createDefinition());

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> context.fail(
                        null));

        assertEquals(
                "Error message must not be null",
                exception.getMessage());
    }

    @Test
    void shouldRejectBlankFailureMessage() {

        WorkflowContext context = WorkflowContext.pending(
                createDefinition());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> context.fail(
                        "   "));

        assertEquals(
                "Error message must not be blank",
                exception.getMessage());
    }

    private WorkflowDefinition createDefinition() {

        WorkflowNode prepareRequest = WorkflowNode.of(
                "prepare-request",
                "Prepare Request",
                "准备法律分析请求");

        WorkflowNode legalAnalysis = WorkflowNode.of(
                "legal-analysis",
                "Legal Analysis",
                "执行法律分析");

        WorkflowNode generateResult = WorkflowNode.of(
                "generate-result",
                "Generate Result",
                "生成最终结果");

        return WorkflowDefinition.of(
                "legal-analysis-workflow",
                "Legal Analysis Workflow",
                "prepare-request",
                List.of(
                        prepareRequest,
                        legalAnalysis,
                        generateResult),
                List.of(
                        WorkflowTransition.of(
                                "prepare-request",
                                "legal-analysis"),
                        WorkflowTransition.of(
                                "legal-analysis",
                                "generate-result")));
    }
}