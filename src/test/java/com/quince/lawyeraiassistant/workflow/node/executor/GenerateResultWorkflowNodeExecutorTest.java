package com.quince.lawyeraiassistant.workflow.node.executor;

import com.quince.lawyeraiassistant.workflow.agent.AgentWorkflowVariables;
import com.quince.lawyeraiassistant.workflow.executor.WorkflowNodeExecutionResult;
import com.quince.lawyeraiassistant.workflow.model.WorkflowContext;
import com.quince.lawyeraiassistant.workflow.model.WorkflowStatus;
import com.quince.lawyeraiassistant.workflow.node.WorkflowNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenerateResultWorkflowNodeExecutorTest {

    private final GenerateResultWorkflowNodeExecutor executor = new GenerateResultWorkflowNodeExecutor();

    @Test
    void shouldSupportGenerateResultNode() {

        assertTrue(
                executor.supports(
                        WorkflowNode.of(
                                "generate-result",
                                "Generate Result",
                                null)));
    }

    @Test
    void shouldNotSupportOtherNode() {

        assertFalse(
                executor.supports(
                        WorkflowNode.of(
                                "other-node",
                                "Other",
                                null)));
    }

    @Test
    void shouldCreateWorkflowResultFromAgentFinalAnswer() {

        WorkflowContext context = WorkflowContext.builder()
                .workflowId(
                        "workflow")
                .currentNodeId(
                        "generate-result")
                .status(
                        WorkflowStatus.RUNNING)
                .variables(
                        Map.of(
                                AgentWorkflowVariables.AGENT_FINAL_ANSWER,
                                "法律分析结果"))
                .nodeStatuses(
                        Map.of())
                .errorMessage(
                        "")
                .build();

        WorkflowNodeExecutionResult result = executor.execute(
                WorkflowNode.of(
                        "generate-result",
                        "Generate Result",
                        null),
                context);

        assertTrue(
                result.isSuccess());

        assertEquals(
                "法律分析结果",
                result.getVariables()
                        .get(
                                "workflowResult"));
    }

    @Test
    void shouldFailWhenAgentFinalAnswerMissing() {

        WorkflowContext context = WorkflowContext.builder()
                .workflowId(
                        "workflow")
                .currentNodeId(
                        "generate-result")
                .status(
                        WorkflowStatus.RUNNING)
                .variables(
                        Map.of())
                .nodeStatuses(
                        Map.of())
                .errorMessage(
                        "")
                .build();

        WorkflowNodeExecutionResult result = executor.execute(
                WorkflowNode.of(
                        "generate-result",
                        "Generate Result",
                        null),
                context);

        assertFalse(
                result.isSuccess());

        assertEquals(
                "Agent final answer is missing",
                result.getErrorMessage());
    }
}