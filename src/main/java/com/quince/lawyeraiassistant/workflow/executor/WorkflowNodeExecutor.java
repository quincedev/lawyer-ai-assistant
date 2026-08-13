package com.quince.lawyeraiassistant.workflow.executor;

import com.quince.lawyeraiassistant.workflow.model.WorkflowContext;
import com.quince.lawyeraiassistant.workflow.node.WorkflowNode;

/**
 * Workflow Node 执行器。
 */
public interface WorkflowNodeExecutor {

    /**
     * 当前 Executor 是否支持执行该 Node。
     */
    boolean supports(
            WorkflowNode node);

    /**
     * 执行 Node。
     */
    WorkflowNodeExecutionResult execute(
            WorkflowNode node,
            WorkflowContext context);
}