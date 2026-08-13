package com.quince.lawyeraiassistant.workflow.condition;

import com.quince.lawyeraiassistant.workflow.model.WorkflowContext;

/**
 * Workflow Transition 的条件判断。
 */
@FunctionalInterface
public interface WorkflowCondition {

    /**
     * 判断当前 Transition 是否允许执行。
     */
    boolean matches(
            WorkflowContext context);
}