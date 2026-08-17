package com.quince.lawyeraiassistant.security.runtime.resource.policy;

import java.util.Objects;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.quince.lawyeraiassistant.security.runtime.AgentExecutionLimits;
import com.quince.lawyeraiassistant.security.runtime.resource.RuntimeResourceGuardrailPolicy;
import com.quince.lawyeraiassistant.security.runtime.resource.RuntimeResourceResult;
import com.quince.lawyeraiassistant.security.runtime.resource.RuntimeResourceType;

@Component
@Order(10)
public final class ObservationLengthRuntimeResourcePolicy
        implements RuntimeResourceGuardrailPolicy {

    private static final String POLICY_NAME = "observationLengthLimit";

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

        if (resourceType != RuntimeResourceType.OBSERVATION) {

            return RuntimeResourceResult.allow(
                    POLICY_NAME);
        }

        if (resourceLength > limits.maxObservationLength()) {

            return RuntimeResourceResult.deny(
                    POLICY_NAME,
                    "Maximum Observation length exceeded");
        }

        return RuntimeResourceResult.allow(
                POLICY_NAME);
    }
}