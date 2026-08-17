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
class ReplanLimitRuntimeGuardrailPolicyTest {

    @Test
    void shouldAllowReplanWhenBudgetRemains() {

        AgentExecutionBudget budget = new AgentExecutionBudget(
                createLimits(
                        2));

        budget.recordReplan();

        RuntimeGuardrailResult result = new ReplanLimitRuntimeGuardrailPolicy()
                .evaluate(
                        RuntimeGuardrailOperation.REPLAN,
                        budget);

        assertTrue(
                result.isAllowed());
    }

    @Test
    void shouldDenyReplanWhenLimitReached() {

        AgentExecutionBudget budget = new AgentExecutionBudget(
                createLimits(
                        2));

        budget.recordReplan();
        budget.recordReplan();

        RuntimeGuardrailResult result = new ReplanLimitRuntimeGuardrailPolicy()
                .evaluate(
                        RuntimeGuardrailOperation.REPLAN,
                        budget);

        assertTrue(
                result.isDenied());
    }

    @Test
    void shouldDenyFirstReplanWhenLimitIsZero() {

        AgentExecutionBudget budget = new AgentExecutionBudget(
                createLimits(
                        0));

        RuntimeGuardrailResult result = new ReplanLimitRuntimeGuardrailPolicy()
                .evaluate(
                        RuntimeGuardrailOperation.REPLAN,
                        budget);

        assertTrue(
                result.isDenied());
    }

    private AgentExecutionLimits createLimits(
            int maxReplans) {

        return new AgentExecutionLimits(
                10,
                5,
                maxReplans,
                2,
                Duration.ofSeconds(60),
                Duration.ofSeconds(10),
                20_000,
                60_000);
    }
}