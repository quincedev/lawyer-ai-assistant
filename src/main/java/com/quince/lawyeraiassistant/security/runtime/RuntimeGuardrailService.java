package com.quince.lawyeraiassistant.security.runtime;

public interface RuntimeGuardrailService {

    RuntimeGuardrailResult evaluate(
            RuntimeGuardrailOperation operation,
            AgentExecutionBudget budget);
}