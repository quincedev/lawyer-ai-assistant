package com.quince.lawyeraiassistant.security.runtime.policy;

import com.quince.lawyeraiassistant.security.runtime.AgentExecutionBudget;
import com.quince.lawyeraiassistant.security.runtime.AgentExecutionLimits;
import com.quince.lawyeraiassistant.security.runtime.RuntimeGuardrailOperation;
import com.quince.lawyeraiassistant.security.runtime.RuntimeGuardrailResult;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import com.quince.lawyeraiassistant.security.SecurityTest;

@SecurityTest
class RetryLimitRuntimeGuardrailPolicyTest {

    @Test
    void shouldAllowRetryWhenBudgetRemains() {

        AgentExecutionBudget budget = new AgentExecutionBudget(
                createLimits(
                        2));

        budget.recordRetry();

        RuntimeGuardrailResult result = new RetryLimitRuntimeGuardrailPolicy()
                .evaluate(
                        RuntimeGuardrailOperation.RETRY,
                        budget);

        assertTrue(
                result.isAllowed());
    }

    @Test
    void shouldDenyRetryWhenLimitReached() {

        AgentExecutionBudget budget = new AgentExecutionBudget(
                createLimits(
                        2));

        budget.recordRetry();
        budget.recordRetry();

        RuntimeGuardrailResult result = new RetryLimitRuntimeGuardrailPolicy()
                .evaluate(
                        RuntimeGuardrailOperation.RETRY,
                        budget);

        assertTrue(
                result.isDenied());
    }

    @Test
    void shouldDenyFirstRetryWhenLimitIsZero() {

        AgentExecutionBudget budget = new AgentExecutionBudget(
                createLimits(
                        0));

        RuntimeGuardrailResult result = new RetryLimitRuntimeGuardrailPolicy()
                .evaluate(
                        RuntimeGuardrailOperation.RETRY,
                        budget);

        assertTrue(
                result.isDenied());
    }

    private AgentExecutionLimits createLimits(
            int maxRetries) {

        return new AgentExecutionLimits(
                10,
                5,
                2,
                maxRetries,
                Duration.ofSeconds(60),
                Duration.ofSeconds(10),
                20_000,
                60_000);
    }
}