package com.quince.lawyeraiassistant.security.runtime.resource;

import com.quince.lawyeraiassistant.security.SecurityTest;
import com.quince.lawyeraiassistant.security.runtime.AgentExecutionLimits;

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

@SecurityTest
class DefaultRuntimeResourceGuardrailServiceTest {

    @Test
    void shouldAllowWhenAllPoliciesAllow() {

        RuntimeResourceGuardrailPolicy first = mock(RuntimeResourceGuardrailPolicy.class);

        RuntimeResourceGuardrailPolicy second = mock(RuntimeResourceGuardrailPolicy.class);

        AgentExecutionLimits limits = createLimits();

        when(
                first.evaluate(
                        RuntimeResourceType.CONTEXT,
                        100,
                        limits))
                .thenReturn(
                        RuntimeResourceResult.allow(
                                "first"));

        when(
                second.evaluate(
                        RuntimeResourceType.CONTEXT,
                        100,
                        limits))
                .thenReturn(
                        RuntimeResourceResult.allow(
                                "second"));

        DefaultRuntimeResourceGuardrailService service = new DefaultRuntimeResourceGuardrailService(
                List.of(
                        first,
                        second),
                limits);

        RuntimeResourceResult result = service.evaluate(
                RuntimeResourceType.CONTEXT,
                100);

        assertTrue(
                result.isAllowed());

        assertEquals(
                "runtimeResourceGuardrail",
                result.policyName());
    }

    @Test
    void shouldStopAtFirstDeny() {

        RuntimeResourceGuardrailPolicy first = mock(RuntimeResourceGuardrailPolicy.class);

        RuntimeResourceGuardrailPolicy second = mock(RuntimeResourceGuardrailPolicy.class);

        AgentExecutionLimits limits = createLimits();

        when(
                first.evaluate(
                        RuntimeResourceType.OBSERVATION,
                        30_000,
                        limits))
                .thenReturn(
                        RuntimeResourceResult.deny(
                                "first",
                                "Too large"));

        DefaultRuntimeResourceGuardrailService service = new DefaultRuntimeResourceGuardrailService(
                List.of(
                        first,
                        second),
                limits);

        RuntimeResourceResult result = service.evaluate(
                RuntimeResourceType.OBSERVATION,
                30_000);

        assertTrue(
                result.isDenied());

        assertEquals(
                "first",
                result.policyName());

        verify(
                second,
                never())
                .evaluate(
                        RuntimeResourceType.OBSERVATION,
                        30_000,
                        limits);
    }

    @Test
    void shouldRejectNegativeResourceLength() {

        DefaultRuntimeResourceGuardrailService service = new DefaultRuntimeResourceGuardrailService(
                List.of(
                        mock(
                                RuntimeResourceGuardrailPolicy.class)),
                createLimits());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.evaluate(
                        RuntimeResourceType.CONTEXT,
                        -1));
    }

    @Test
    void shouldRejectEmptyPolicyList() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new DefaultRuntimeResourceGuardrailService(
                        List.of(),
                        createLimits()));
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
