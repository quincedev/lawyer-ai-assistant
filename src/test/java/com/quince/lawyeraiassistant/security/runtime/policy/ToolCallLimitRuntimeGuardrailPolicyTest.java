package com.quince.lawyeraiassistant.security.runtime.policy;

import com.quince.lawyeraiassistant.security.runtime.AgentExecutionBudget;
import com.quince.lawyeraiassistant.security.runtime.AgentExecutionLimits;
import com.quince.lawyeraiassistant.security.runtime.RuntimeGuardrailOperation;
import com.quince.lawyeraiassistant.security.runtime.RuntimeGuardrailResult;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.quince.lawyeraiassistant.security.SecurityTest;

@SecurityTest
class ToolCallLimitRuntimeGuardrailPolicyTest {

    private final ToolCallLimitRuntimeGuardrailPolicy policy = new ToolCallLimitRuntimeGuardrailPolicy();

    @Test
    void shouldAllowToolCallWhenBudgetRemains() {

        AgentExecutionBudget budget = new AgentExecutionBudget(
                createLimits(2));

        budget.recordToolCall();

        RuntimeGuardrailResult result = policy.evaluate(
                RuntimeGuardrailOperation.TOOL_CALL,
                budget);

        assertTrue(
                result.isAllowed());
    }

    @Test
    void shouldDenyToolCallWhenLimitReached() {

        AgentExecutionBudget budget = new AgentExecutionBudget(
                createLimits(2));

        budget.recordToolCall();
        budget.recordToolCall();

        RuntimeGuardrailResult result = policy.evaluate(
                RuntimeGuardrailOperation.TOOL_CALL,
                budget);

        assertTrue(
                result.isDenied());

        assertEquals(
                "toolCallLimit",
                result.policyName());

        assertEquals(
                "Maximum Agent tool calls reached",
                result.reason());
    }

    @Test
    void shouldIgnoreNonToolCallOperation() {

        AgentExecutionBudget budget = new AgentExecutionBudget(
                createLimits(1));

        budget.recordToolCall();

        RuntimeGuardrailResult result = policy.evaluate(
                RuntimeGuardrailOperation.STEP,
                budget);

        assertTrue(
                result.isAllowed());
    }

    private AgentExecutionLimits createLimits(
            int maxToolCalls) {

        return new AgentExecutionLimits(
                10,
                maxToolCalls,
                2,
                3,
                Duration.ofSeconds(120),
                Duration.ofSeconds(30),
                20_000,
                60_000);
    }
}