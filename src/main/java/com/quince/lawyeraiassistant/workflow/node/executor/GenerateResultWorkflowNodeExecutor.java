package com.quince.lawyeraiassistant.workflow.node.executor;

import com.quince.lawyeraiassistant.workflow.agent.AgentWorkflowVariables;
import com.quince.lawyeraiassistant.workflow.executor.WorkflowNodeExecutionResult;
import com.quince.lawyeraiassistant.workflow.executor.WorkflowNodeExecutor;
import com.quince.lawyeraiassistant.workflow.model.WorkflowContext;
import com.quince.lawyeraiassistant.workflow.node.WorkflowNode;

import java.util.Map;

public final class GenerateResultWorkflowNodeExecutor
        implements WorkflowNodeExecutor {

    private static final String NODE_ID =
            "generate-result";

    @Override
    public boolean supports(
            WorkflowNode node) {

        return NODE_ID.equals(
                node.getId());
    }

    @Override
    public WorkflowNodeExecutionResult execute(
            WorkflowNode node,
            WorkflowContext context) {

        Object answer =
                context.getVariable(
                        AgentWorkflowVariables.AGENT_FINAL_ANSWER);

        if (!(answer instanceof String value)
                || value.isBlank()) {

            return WorkflowNodeExecutionResult.failure(
                    "Agent final answer is missing");
        }

        return WorkflowNodeExecutionResult.success(
                Map.of(
                        "workflowResult",
                        value));
    }
}