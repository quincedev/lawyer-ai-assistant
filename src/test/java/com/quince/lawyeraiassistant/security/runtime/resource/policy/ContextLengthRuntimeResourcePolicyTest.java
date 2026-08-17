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
class ContextLengthRuntimeResourcePolicyTest {

    private final ContextLengthRuntimeResourcePolicy policy = new ContextLengthRuntimeResourcePolicy();

    @Test
    void shouldAllowContextBelowLimit() {

        RuntimeResourceResult result = policy.evaluate(
                RuntimeResourceType.CONTEXT,
                999,
                createLimits(1000));

        assertTrue(
                result.isAllowed());
    }

    @Test
    void shouldAllowContextExactlyAtLimit() {

        RuntimeResourceResult result = policy.evaluate(
                RuntimeResourceType.CONTEXT,
                1000,
                createLimits(1000));

        assertTrue(
                result.isAllowed());
    }

    @Test
    void shouldDenyContextAboveLimit() {

        RuntimeResourceResult result = policy.evaluate(
                RuntimeResourceType.CONTEXT,
                1001,
                createLimits(1000));

        assertTrue(
                result.isDenied());

        assertEquals(
                "contextLengthLimit",
                result.policyName());

        assertEquals(
                "Maximum Agent context length exceeded",
                result.reason());
    }

    @Test
    void shouldIgnoreObservationResource() {

        RuntimeResourceResult result = policy.evaluate(
                RuntimeResourceType.OBSERVATION,
                100_000,
                createLimits(1000));

        assertTrue(
                result.isAllowed());
    }

    private AgentExecutionLimits createLimits(
            int maxContextLength) {

        return new AgentExecutionLimits(
                10,
                8,
                2,
                3,
                Duration.ofSeconds(120),
                Duration.ofSeconds(30),
                20_000,
                maxContextLength);
    }
}
