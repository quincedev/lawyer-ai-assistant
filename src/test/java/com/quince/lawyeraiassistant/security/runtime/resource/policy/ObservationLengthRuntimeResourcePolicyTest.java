package com.quince.lawyeraiassistant.security.runtime.resource.policy;

import com.quince.lawyeraiassistant.security.SecurityTest;
import com.quince.lawyeraiassistant.security.runtime.AgentExecutionLimits;
import com.quince.lawyeraiassistant.security.runtime.resource.RuntimeResourceResult;
import com.quince.lawyeraiassistant.security.runtime.resource.RuntimeResourceType;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SecurityTest
class ObservationLengthRuntimeResourcePolicyTest {

    private final ObservationLengthRuntimeResourcePolicy policy = new ObservationLengthRuntimeResourcePolicy();

    @Test
    void shouldAllowObservationBelowLimit() {

        RuntimeResourceResult result = policy.evaluate(
                RuntimeResourceType.OBSERVATION,
                99,
                createLimits(100));

        assertTrue(
                result.isAllowed());
    }

    @Test
    void shouldAllowObservationExactlyAtLimit() {

        RuntimeResourceResult result = policy.evaluate(
                RuntimeResourceType.OBSERVATION,
                100,
                createLimits(100));

        assertTrue(
                result.isAllowed());
    }

    @Test
    void shouldDenyObservationAboveLimit() {

        RuntimeResourceResult result = policy.evaluate(
                RuntimeResourceType.OBSERVATION,
                101,
                createLimits(100));

        assertTrue(
                result.isDenied());

        assertEquals(
                "observationLengthLimit",
                result.policyName());

        assertEquals(
                "Maximum Observation length exceeded",
                result.reason());
    }

    @Test
    void shouldIgnoreContextResource() {

        RuntimeResourceResult result = policy.evaluate(
                RuntimeResourceType.CONTEXT,
                100_000,
                createLimits(100));

        assertTrue(
                result.isAllowed());
    }

    private AgentExecutionLimits createLimits(
            int maxObservationLength) {

        return new AgentExecutionLimits(
                10,
                8,
                2,
                3,
                Duration.ofSeconds(120),
                Duration.ofSeconds(30),
                maxObservationLength,
                60_000);
    }
}
