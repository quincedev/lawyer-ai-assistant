package com.quince.lawyeraiassistant.workflow.executor;

import com.quince.lawyeraiassistant.workflow.model.WorkflowContext;
import com.quince.lawyeraiassistant.workflow.model.WorkflowDefinition;

/**
 * Workflow 执行入口。
 */
public interface WorkflowExecutor {

    WorkflowContext execute(
            WorkflowDefinition definition,
            WorkflowContext context);
}