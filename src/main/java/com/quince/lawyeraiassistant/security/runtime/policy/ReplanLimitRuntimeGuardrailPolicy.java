package com.quince.lawyeraiassistant.security.runtime.policy;

import com.quince.lawyeraiassistant.security.runtime.AgentExecutionBudget;
import com.quince.lawyeraiassistant.security.runtime.RuntimeGuardrailOperation;
import com.quince.lawyeraiassistant.security.runtime.RuntimeGuardrailPolicy;
import com.quince.lawyeraiassistant.security.runtime.RuntimeGuardrailResult;

import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public final class ReplanLimitRuntimeGuardrailPolicy
        implements RuntimeGuardrailPolicy {

    static final String POLICY_NAME = "replanLimit";

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

        if (operation != RuntimeGuardrailOperation.REPLAN) {

            return RuntimeGuardrailResult.allow(
                    POLICY_NAME);
        }

        if (budget.replansUsed() >= budget.limits().maxReplans()) {

            return RuntimeGuardrailResult.deny(
                    POLICY_NAME,
                    "Maximum Agent replans reached");
        }

        return RuntimeGuardrailResult.allow(
                POLICY_NAME);
    }
}