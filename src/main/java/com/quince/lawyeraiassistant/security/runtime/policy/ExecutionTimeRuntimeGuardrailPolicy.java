package com.quince.lawyeraiassistant.security.runtime.policy;

import com.quince.lawyeraiassistant.security.runtime.AgentExecutionBudget;
import com.quince.lawyeraiassistant.security.runtime.RuntimeGuardrailOperation;
import com.quince.lawyeraiassistant.security.runtime.RuntimeGuardrailPolicy;
import com.quince.lawyeraiassistant.security.runtime.RuntimeGuardrailResult;

import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public final class ExecutionTimeRuntimeGuardrailPolicy
        implements RuntimeGuardrailPolicy {

    static final String POLICY_NAME = "executionTimeLimit";

    @Override
    public RuntimeGuardrailResult evaluate(
            RuntimeGuardrailOperation operation,
            AgentExecutionBudget budget) {

        Objects.requireNonNull(
                operation,
                "operation must not be null");

        Objects.requireNonNull(
                budget,
                "budget must not be null");

        if (budget.elapsed()
                .compareTo(
                        budget.limits()
                                .maxExecutionTime()) >= 0) {

            return RuntimeGuardrailResult.deny(
                    POLICY_NAME,
                    "Maximum Agent execution time reached");
        }

        return RuntimeGuardrailResult.allow(
                POLICY_NAME);
    }
}