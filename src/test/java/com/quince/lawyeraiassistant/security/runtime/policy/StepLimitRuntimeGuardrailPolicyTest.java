package com.quince.lawyeraiassistant.security.runtime.policy;

import com.quince.lawyeraiassistant.security.runtime.AgentExecutionBudget;
import com.quince.lawyeraiassistant.security.runtime.AgentExecutionLimits;
import com.quince.lawyeraiassistant.security.runtime.RuntimeGuardrailOperation;
import com.quince.lawyeraiassistant.security.runtime.RuntimeGuardrailResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.quince.lawyeraiassistant.security.SecurityTest;

@SecurityTest
class StepLimitRuntimeGuardrailPolicyTest {

    private StepLimitRuntimeGuardrailPolicy policy;

    @BeforeEach
    void setUp() {

        policy = new StepLimitRuntimeGuardrailPolicy();
    }

    @Test
    void shouldAllowStepWhenBudgetRemains() {

        AgentExecutionBudget budget = new AgentExecutionBudget(
                createLimits());

        budget.recordStep();
        budget.recordStep();

        RuntimeGuardrailResult result = policy.evaluate(
                RuntimeGuardrailOperation.STEP,
                budget);

        assertTrue(
                result.isAllowed());
    }

    @Test
    void shouldDenyStepWhenLimitReached() {

        AgentExecutionBudget budget = new AgentExecutionBudget(
                createLimits());

        budget.recordStep();
        budget.recordStep();
        budget.recordStep();

        RuntimeGuardrailResult result = policy.evaluate(
                RuntimeGuardrailOperation.STEP,
                budget);

        assertTrue(
                result.isDenied());

        assertEquals(
                "stepLimit",
                result.policyName());

        assertEquals(
                "Maximum Agent execution steps reached",
                result.reason());
    }

    @Test
    void shouldIgnoreOtherOperations() {

        AgentExecutionBudget budget = new AgentExecutionBudget(
                createLimits());

        budget.recordStep();
        budget.recordStep();
        budget.recordStep();

        RuntimeGuardrailResult result = policy.evaluate(
                RuntimeGuardrailOperation.REPLAN,
                budget);

        assertTrue(
                result.isAllowed());
    }

    private AgentExecutionLimits createLimits() {

        return new AgentExecutionLimits(
                3,
                5,
                2,
                2,
                Duration.ofSeconds(60),
                Duration.ofSeconds(10),
                20_000,
                60_000);
    }
}