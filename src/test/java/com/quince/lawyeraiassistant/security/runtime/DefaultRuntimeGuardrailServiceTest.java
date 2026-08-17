package com.quince.lawyeraiassistant.security.runtime;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.quince.lawyeraiassistant.security.SecurityTest;

@SecurityTest
class DefaultRuntimeGuardrailServiceTest {

    @Test
    void shouldAllowWhenAllPoliciesAllow() {

        RuntimeGuardrailPolicy first = mock(RuntimeGuardrailPolicy.class);

        RuntimeGuardrailPolicy second = mock(RuntimeGuardrailPolicy.class);

        AgentExecutionBudget budget = new AgentExecutionBudget(
                createLimits());

        when(
                first.evaluate(
                        RuntimeGuardrailOperation.STEP,
                        budget))
                .thenReturn(
                        RuntimeGuardrailResult.allow(
                                "first"));

        when(
                second.evaluate(
                        RuntimeGuardrailOperation.STEP,
                        budget))
                .thenReturn(
                        RuntimeGuardrailResult.allow(
                                "second"));

        DefaultRuntimeGuardrailService service = new DefaultRuntimeGuardrailService(
                List.of(
                        first,
                        second));

        RuntimeGuardrailResult result = service.evaluate(
                RuntimeGuardrailOperation.STEP,
                budget);

        assertTrue(
                result.isAllowed());

        assertEquals(
                "runtimeGuardrail",
                result.policyName());

        verify(first)
                .evaluate(
                        RuntimeGuardrailOperation.STEP,
                        budget);

        verify(second)
                .evaluate(
                        RuntimeGuardrailOperation.STEP,
                        budget);
    }

    @Test
    void shouldStopAtFirstDeny() {

        RuntimeGuardrailPolicy first = mock(RuntimeGuardrailPolicy.class);

        RuntimeGuardrailPolicy second = mock(RuntimeGuardrailPolicy.class);

        RuntimeGuardrailPolicy third = mock(RuntimeGuardrailPolicy.class);

        AgentExecutionBudget budget = new AgentExecutionBudget(
                createLimits());

        when(
                first.evaluate(
                        RuntimeGuardrailOperation.STEP,
                        budget))
                .thenReturn(
                        RuntimeGuardrailResult.allow(
                                "first"));

        when(
                second.evaluate(
                        RuntimeGuardrailOperation.STEP,
                        budget))
                .thenReturn(
                        RuntimeGuardrailResult.deny(
                                "second",
                                "Denied"));

        DefaultRuntimeGuardrailService service = new DefaultRuntimeGuardrailService(
                List.of(
                        first,
                        second,
                        third));

        RuntimeGuardrailResult result = service.evaluate(
                RuntimeGuardrailOperation.STEP,
                budget);

        assertTrue(
                result.isDenied());

        assertEquals(
                "second",
                result.policyName());

        assertEquals(
                "Denied",
                result.reason());

        verify(
                third,
                never())
                .evaluate(
                        RuntimeGuardrailOperation.STEP,
                        budget);
    }

    @Test
    void shouldRejectEmptyPolicyList() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new DefaultRuntimeGuardrailService(
                        List.of()));
    }

    @Test
    void shouldRejectNullPolicyResult() {

        RuntimeGuardrailPolicy policy = mock(RuntimeGuardrailPolicy.class);

        AgentExecutionBudget budget = new AgentExecutionBudget(
                createLimits());

        when(
                policy.evaluate(
                        RuntimeGuardrailOperation.STEP,
                        budget))
                .thenReturn(
                        null);

        DefaultRuntimeGuardrailService service = new DefaultRuntimeGuardrailService(
                List.of(
                        policy));

        assertThrows(
                NullPointerException.class,
                () -> service.evaluate(
                        RuntimeGuardrailOperation.STEP,
                        budget));
    }

    private AgentExecutionLimits createLimits() {

        return new AgentExecutionLimits(
                10,
                8,
                2,
                3,
                Duration.ofSeconds(120),
                Duration.ofSeconds(30),
                20_000,
                60_000);
    }
}