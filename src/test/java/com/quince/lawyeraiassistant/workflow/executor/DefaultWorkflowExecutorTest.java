package com.quince.lawyeraiassistant.workflow.executor;

import com.quince.lawyeraiassistant.workflow.model.WorkflowContext;
import com.quince.lawyeraiassistant.workflow.model.WorkflowDefinition;
import com.quince.lawyeraiassistant.workflow.model.WorkflowNodeStatus;
import com.quince.lawyeraiassistant.workflow.model.WorkflowStatus;
import com.quince.lawyeraiassistant.workflow.node.WorkflowNode;
import com.quince.lawyeraiassistant.workflow.transition.WorkflowTransition;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultWorkflowExecutorTest {

    @Test
    void shouldFollowHighRiskBranch() {

        WorkflowDefinition definition = createRiskWorkflow();

        WorkflowContext context = WorkflowContext.pending(
                definition);

        List<String> executedNodes = new ArrayList<>();

        WorkflowNodeExecutor nodeExecutor = new WorkflowNodeExecutor() {

            @Override
            public boolean supports(
                    WorkflowNode node) {

                return true;
            }

            @Override
            public WorkflowNodeExecutionResult execute(
                    WorkflowNode node,
                    WorkflowContext context) {

                executedNodes.add(
                        node.getId());

                if ("analyze-risk".equals(
                        node.getId())) {

                    return WorkflowNodeExecutionResult.success(
                            Map.of(
                                    "riskLevel",
                                    "HIGH"));
                }

                return WorkflowNodeExecutionResult.success();
            }
        };

        DefaultWorkflowExecutor executor = new DefaultWorkflowExecutor(
                List.of(
                        nodeExecutor));

        WorkflowContext result = executor.execute(
                definition,
                context);

        assertEquals(
                WorkflowStatus.COMPLETED,
                result.getStatus());

        assertEquals(
                List.of(
                        "analyze-risk",
                        "human-review",
                        "generate-report"),
                executedNodes);

        assertEquals(
                WorkflowNodeStatus.COMPLETED,
                result.getNodeStatus(
                        "analyze-risk"));

        assertEquals(
                WorkflowNodeStatus.COMPLETED,
                result.getNodeStatus(
                        "human-review"));

        assertEquals(
                WorkflowNodeStatus.COMPLETED,
                result.getNodeStatus(
                        "generate-report"));
    }

    @Test
    void shouldFollowLowRiskBranch() {

        WorkflowDefinition definition = createRiskWorkflow();

        WorkflowContext context = WorkflowContext.pending(
                definition);

        List<String> executedNodes = new ArrayList<>();

        WorkflowNodeExecutor nodeExecutor = new WorkflowNodeExecutor() {

            @Override
            public boolean supports(
                    WorkflowNode node) {

                return true;
            }

            @Override
            public WorkflowNodeExecutionResult execute(
                    WorkflowNode node,
                    WorkflowContext context) {

                executedNodes.add(
                        node.getId());

                if ("analyze-risk".equals(
                        node.getId())) {

                    return WorkflowNodeExecutionResult.success(
                            Map.of(
                                    "riskLevel",
                                    "LOW"));
                }

                return WorkflowNodeExecutionResult.success();
            }
        };

        DefaultWorkflowExecutor executor = new DefaultWorkflowExecutor(
                List.of(
                        nodeExecutor));

        WorkflowContext result = executor.execute(
                definition,
                context);

        assertEquals(
                WorkflowStatus.COMPLETED,
                result.getStatus());

        assertEquals(
                List.of(
                        "analyze-risk",
                        "generate-report"),
                executedNodes);

        assertEquals(
                WorkflowNodeStatus.PENDING,
                result.getNodeStatus(
                        "human-review"));
    }

    @Test
    void shouldFailWhenNoTransitionConditionMatches() {

        WorkflowDefinition definition = createRiskWorkflow();

        WorkflowContext context = WorkflowContext.pending(
                definition);

        WorkflowNodeExecutor nodeExecutor = new WorkflowNodeExecutor() {

            @Override
            public boolean supports(
                    WorkflowNode node) {

                return true;
            }

            @Override
            public WorkflowNodeExecutionResult execute(
                    WorkflowNode node,
                    WorkflowContext context) {

                if ("analyze-risk".equals(
                        node.getId())) {

                    return WorkflowNodeExecutionResult.success(
                            Map.of(
                                    "riskLevel",
                                    "MEDIUM"));
                }

                return WorkflowNodeExecutionResult.success();
            }
        };

        DefaultWorkflowExecutor executor = new DefaultWorkflowExecutor(
                List.of(
                        nodeExecutor));

        WorkflowContext result = executor.execute(
                definition,
                context);

        assertEquals(
                WorkflowStatus.FAILED,
                result.getStatus());

        assertEquals(
                "No matching transition found for node: analyze-risk",
                result.getErrorMessage());

        assertEquals(
                WorkflowNodeStatus.COMPLETED,
                result.getNodeStatus(
                        "analyze-risk"));
    }

    @Test
    void shouldFollowDefaultTransitionWhenSpecificConditionDoesNotMatch() {

        WorkflowNode analyzeRisk = WorkflowNode.of(
                "analyze-risk",
                "Analyze Risk",
                null);

        WorkflowNode humanReview = WorkflowNode.of(
                "human-review",
                "Human Review",
                null);

        WorkflowNode generateReport = WorkflowNode.of(
                "generate-report",
                "Generate Report",
                null);

        WorkflowDefinition definition = WorkflowDefinition.of(
                "default-transition-workflow",
                "Default Transition Workflow",
                "analyze-risk",
                List.of(
                        analyzeRisk,
                        humanReview,
                        generateReport),
                List.of(
                        WorkflowTransition.when(
                                "analyze-risk",
                                "human-review",
                                context -> "HIGH".equals(
                                        context.getVariable(
                                                "riskLevel"))),
                        WorkflowTransition.of(
                                "analyze-risk",
                                "generate-report")));

        List<String> executedNodes = new ArrayList<>();

        WorkflowNodeExecutor nodeExecutor = new WorkflowNodeExecutor() {

            @Override
            public boolean supports(
                    WorkflowNode node) {

                return true;
            }

            @Override
            public WorkflowNodeExecutionResult execute(
                    WorkflowNode node,
                    WorkflowContext context) {

                executedNodes.add(
                        node.getId());

                if ("analyze-risk".equals(
                        node.getId())) {

                    return WorkflowNodeExecutionResult.success(
                            Map.of(
                                    "riskLevel",
                                    "MEDIUM"));
                }

                return WorkflowNodeExecutionResult.success();
            }
        };

        DefaultWorkflowExecutor executor = new DefaultWorkflowExecutor(
                List.of(
                        nodeExecutor));

        WorkflowContext result = executor.execute(
                definition,
                WorkflowContext.pending(
                        definition));

        assertEquals(
                WorkflowStatus.COMPLETED,
                result.getStatus());

        assertEquals(
                List.of(
                        "analyze-risk",
                        "generate-report"),
                executedNodes);
    }

    @Test
    void shouldExposeLatestNodeVariablesToTransitionCondition() {

        WorkflowNode analyzeRisk = WorkflowNode.of(
                "analyze-risk",
                "Analyze Risk",
                null);

        WorkflowNode humanReview = WorkflowNode.of(
                "human-review",
                "Human Review",
                null);

        WorkflowDefinition definition = WorkflowDefinition.of(
                "latest-context-workflow",
                "Latest Context Workflow",
                "analyze-risk",
                List.of(
                        analyzeRisk,
                        humanReview),
                List.of(
                        WorkflowTransition.when(
                                "analyze-risk",
                                "human-review",
                                context -> "HIGH".equals(
                                        context.getVariable(
                                                "riskLevel")))));

        WorkflowNodeExecutor nodeExecutor = new WorkflowNodeExecutor() {

            @Override
            public boolean supports(
                    WorkflowNode node) {

                return true;
            }

            @Override
            public WorkflowNodeExecutionResult execute(
                    WorkflowNode node,
                    WorkflowContext context) {

                if ("analyze-risk".equals(
                        node.getId())) {

                    return WorkflowNodeExecutionResult.success(
                            Map.of(
                                    "riskLevel",
                                    "HIGH"));
                }

                return WorkflowNodeExecutionResult.success();
            }
        };

        DefaultWorkflowExecutor executor = new DefaultWorkflowExecutor(
                List.of(
                        nodeExecutor));

        WorkflowContext result = executor.execute(
                definition,
                WorkflowContext.pending(
                        definition));

        assertEquals(
                WorkflowStatus.COMPLETED,
                result.getStatus());

        assertEquals(
                "HIGH",
                result.getVariable(
                        "riskLevel"));

        assertEquals(
                WorkflowNodeStatus.COMPLETED,
                result.getNodeStatus(
                        "human-review"));
    }

    @Test
    void shouldStoreNodeFailureReasonInWorkflowContext() {

        WorkflowNode nodeA = WorkflowNode.of(
                "node-a",
                "Node A",
                null);

        WorkflowNode nodeB = WorkflowNode.of(
                "node-b",
                "Node B",
                null);

        WorkflowDefinition definition = WorkflowDefinition.of(
                "failure-workflow",
                "Failure Workflow",
                "node-a",
                List.of(
                        nodeA,
                        nodeB),
                List.of(
                        WorkflowTransition.of(
                                "node-a",
                                "node-b")));

        WorkflowNodeExecutor nodeExecutor = new WorkflowNodeExecutor() {

            @Override
            public boolean supports(
                    WorkflowNode node) {

                return true;
            }

            @Override
            public WorkflowNodeExecutionResult execute(
                    WorkflowNode node,
                    WorkflowContext context) {

                if ("node-b".equals(
                        node.getId())) {

                    return WorkflowNodeExecutionResult.failure(
                            "node-b failed");
                }

                return WorkflowNodeExecutionResult.success();
            }
        };

        DefaultWorkflowExecutor executor = new DefaultWorkflowExecutor(
                List.of(
                        nodeExecutor));

        WorkflowContext result = executor.execute(
                definition,
                WorkflowContext.pending(
                        definition));

        assertEquals(
                WorkflowStatus.FAILED,
                result.getStatus());

        assertEquals(
                "node-b failed",
                result.getErrorMessage());

        assertEquals(
                WorkflowNodeStatus.COMPLETED,
                result.getNodeStatus(
                        "node-a"));

        assertEquals(
                WorkflowNodeStatus.FAILED,
                result.getNodeStatus(
                        "node-b"));
    }

    @Test
    void shouldRejectWorkflowContextFromDifferentDefinition() {

        WorkflowDefinition definition = createRiskWorkflow();

        WorkflowContext context = WorkflowContext.builder()
                .workflowId(
                        "other-workflow")
                .currentNodeId(
                        "analyze-risk")
                .status(
                        WorkflowStatus.PENDING)
                .variables(
                        Map.of())
                .nodeStatuses(
                        Map.of())
                .build();

        DefaultWorkflowExecutor executor = new DefaultWorkflowExecutor(
                List.of(
                        new SuccessfulExecutor()));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> executor.execute(
                        definition,
                        context));

        assertEquals(
                "WorkflowContext does not belong to definition: risk-workflow",
                exception.getMessage());
    }

    private WorkflowDefinition createRiskWorkflow() {

        WorkflowNode analyzeRisk = WorkflowNode.of(
                "analyze-risk",
                "Analyze Risk",
                "分析合同风险");

        WorkflowNode humanReview = WorkflowNode.of(
                "human-review",
                "Human Review",
                "人工审核");

        WorkflowNode generateReport = WorkflowNode.of(
                "generate-report",
                "Generate Report",
                "生成报告");

        return WorkflowDefinition.of(
                "risk-workflow",
                "Risk Workflow",
                "analyze-risk",
                List.of(
                        analyzeRisk,
                        humanReview,
                        generateReport),
                List.of(
                        WorkflowTransition.when(
                                "analyze-risk",
                                "human-review",
                                context -> "HIGH".equals(
                                        context.getVariable(
                                                "riskLevel"))),
                        WorkflowTransition.when(
                                "analyze-risk",
                                "generate-report",
                                context -> "LOW".equals(
                                        context.getVariable(
                                                "riskLevel"))),
                        WorkflowTransition.of(
                                "human-review",
                                "generate-report")));
    }

    private static final class SuccessfulExecutor
            implements WorkflowNodeExecutor {

        @Override
        public boolean supports(
                WorkflowNode node) {

            return true;
        }

        @Override
        public WorkflowNodeExecutionResult execute(
                WorkflowNode node,
                WorkflowContext context) {

            return WorkflowNodeExecutionResult.success();
        }
    }
}