package com.quince.lawyeraiassistant.security.runtime.resource;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.quince.lawyeraiassistant.security.runtime.AgentExecutionLimits;

@Service
public final class DefaultRuntimeResourceGuardrailService
        implements RuntimeResourceGuardrailService {

    private static final String SERVICE_POLICY_NAME = "runtimeResourceGuardrail";

    private final List<RuntimeResourceGuardrailPolicy> policies;

    private final AgentExecutionLimits executionLimits;

    public DefaultRuntimeResourceGuardrailService(
            List<RuntimeResourceGuardrailPolicy> policies,
            AgentExecutionLimits executionLimits) {

        Objects.requireNonNull(
                policies,
                "policies must not be null");

        if (policies.isEmpty()) {

            throw new IllegalArgumentException(
                    "At least one RuntimeResourceGuardrailPolicy is required");
        }

        this.policies = List.copyOf(
                policies);

        this.executionLimits = Objects.requireNonNull(
                executionLimits,
                "executionLimits must not be null");
    }

    @Override
    public RuntimeResourceResult evaluate(
            RuntimeResourceType resourceType,
            int resourceLength) {

        Objects.requireNonNull(
                resourceType,
                "resourceType must not be null");

        if (resourceLength < 0) {

            throw new IllegalArgumentException(
                    "resourceLength must not be negative");
        }

        for (RuntimeResourceGuardrailPolicy policy : policies) {

            RuntimeResourceResult result = Objects.requireNonNull(
                    policy.evaluate(
                            resourceType,
                            resourceLength,
                            executionLimits),
                    "RuntimeResourceGuardrailPolicy must not return null");

            if (result.isDenied()) {

                return result;
            }
        }

        return RuntimeResourceResult.allow(
                SERVICE_POLICY_NAME);
    }
}