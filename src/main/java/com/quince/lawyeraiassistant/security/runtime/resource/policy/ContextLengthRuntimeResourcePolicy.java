package com.quince.lawyeraiassistant.security.runtime.resource.policy;

import java.util.Objects;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.quince.lawyeraiassistant.security.runtime.AgentExecutionLimits;
import com.quince.lawyeraiassistant.security.runtime.resource.RuntimeResourceGuardrailPolicy;
import com.quince.lawyeraiassistant.security.runtime.resource.RuntimeResourceResult;
import com.quince.lawyeraiassistant.security.runtime.resource.RuntimeResourceType;

@Component
@Order(20)
public final class ContextLengthRuntimeResourcePolicy
        implements RuntimeResourceGuardrailPolicy {

    private static final String POLICY_NAME = "contextLengthLimit";

    @Override
    public RuntimeResourceResult evaluate(
            RuntimeResourceType resourceType,
            int resourceLength,
            AgentExecutionLimits limits) {

        Objects.requireNonNull(
                resourceType,
                "resourceType must not be null");

        Objects.requireNonNull(
                limits,
                "limits must not be null");

        if (resourceLength < 0) {

            throw new IllegalArgumentException(
                    "resourceLength must not be negative");
        }

        if (resourceType != RuntimeResourceType.CONTEXT) {

            return RuntimeResourceResult.allow(
                    POLICY_NAME);
        }

        if (resourceLength > limits.maxContextLength()) {

            return RuntimeResourceResult.deny(
                    POLICY_NAME,
                    "Maximum Agent context length exceeded");
        }

        return RuntimeResourceResult.allow(
                POLICY_NAME);
    }
}