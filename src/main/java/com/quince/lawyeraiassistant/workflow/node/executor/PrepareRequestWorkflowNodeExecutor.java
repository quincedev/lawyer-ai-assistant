package com.quince.lawyeraiassistant.workflow.node.executor;

import com.quince.lawyeraiassistant.workflow.executor.WorkflowNodeExecutionResult;
import com.quince.lawyeraiassistant.workflow.executor.WorkflowNodeExecutor;
import com.quince.lawyeraiassistant.workflow.model.WorkflowContext;
import com.quince.lawyeraiassistant.workflow.node.WorkflowNode;

public final class PrepareRequestWorkflowNodeExecutor
        implements WorkflowNodeExecutor {

    private static final String NODE_ID = "prepare-request";

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

        return WorkflowNodeExecutionResult.success();
    }
}