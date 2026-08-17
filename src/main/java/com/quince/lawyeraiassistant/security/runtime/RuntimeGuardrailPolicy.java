package com.quince.lawyeraiassistant.security.runtime;

/**
 * Policy responsible for evaluating whether a runtime operation
 * is allowed to proceed under the current execution budget.
 */
public interface RuntimeGuardrailPolicy {

    RuntimeGuardrailResult evaluate(
            RuntimeGuardrailOperation operation,
            AgentExecutionBudget budget);
}