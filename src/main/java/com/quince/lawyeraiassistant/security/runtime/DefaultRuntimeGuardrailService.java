package com.quince.lawyeraiassistant.security.runtime;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

@Service
public final class DefaultRuntimeGuardrailService
        implements RuntimeGuardrailService {

    private static final String SERVICE_POLICY_NAME = "runtimeGuardrail";

    private final List<RuntimeGuardrailPolicy> policies;

    public DefaultRuntimeGuardrailService(
            List<RuntimeGuardrailPolicy> policies) {

        Objects.requireNonNull(
                policies,
                "policies must not be null");

        if (policies.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one RuntimeGuardrailPolicy is required");
        }

        this.policies = List.copyOf(
                policies);
    }

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

        for (RuntimeGuardrailPolicy policy : policies) {

            RuntimeGuardrailResult result = Objects.requireNonNull(
                    policy.evaluate(
                            operation,
                            budget),
                    "RuntimeGuardrailPolicy must not return null");

            if (result.isDenied()) {
                return result;
            }
        }

        return RuntimeGuardrailResult.allow(
                SERVICE_POLICY_NAME);
    }
}