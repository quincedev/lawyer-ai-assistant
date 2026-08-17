package com.quince.lawyeraiassistant.security.guardrail.input;

import com.quince.lawyeraiassistant.security.SecurityTest;
import com.quince.lawyeraiassistant.security.guardrail.GuardrailResult;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SecurityTest
class InputGuardrailChainTest {

    @Test
    void shouldAllowWhenAllGuardrailsAllow() {

        InputGuardrail first = mock(
                InputGuardrail.class);

        InputGuardrail second = mock(
                InputGuardrail.class);

        when(
                first.evaluate(
                        "正常法律问题"))
                .thenReturn(
                        GuardrailResult.allow(
                                "first"));

        when(
                second.evaluate(
                        "正常法律问题"))
                .thenReturn(
                        GuardrailResult.allow(
                                "second"));

        InputGuardrailChain chain = new InputGuardrailChain(
                List.of(
                        first,
                        second));

        GuardrailResult result = chain.evaluate(
                "正常法律问题");

        assertTrue(
                result.isAllowed());

        assertEquals(
                "inputGuardrailChain",
                result.guardrailName());

        verify(
                first)
                .evaluate(
                        "正常法律问题");

        verify(
                second)
                .evaluate(
                        "正常法律问题");
    }

    @Test
    void shouldReturnFirstBlockedResult() {

        InputGuardrail first = mock(
                InputGuardrail.class);

        InputGuardrail second = mock(
                InputGuardrail.class);

        GuardrailResult blocked = GuardrailResult.block(
                "inputLength",
                "Input exceeds maximum allowed length");

        when(
                first.evaluate(
                        "unsafe"))
                .thenReturn(
                        blocked);

        InputGuardrailChain chain = new InputGuardrailChain(
                List.of(
                        first,
                        second));

        GuardrailResult result = chain.evaluate(
                "unsafe");

        assertEquals(
                blocked,
                result);

        verify(
                first)
                .evaluate(
                        "unsafe");

        verify(
                second,
                never())
                .evaluate(
                        "unsafe");
    }

    @Test
    void shouldStopAfterLaterGuardrailBlocks() {

        InputGuardrail first = mock(
                InputGuardrail.class);

        InputGuardrail second = mock(
                InputGuardrail.class);

        InputGuardrail third = mock(
                InputGuardrail.class);

        when(
                first.evaluate(
                        "attack"))
                .thenReturn(
                        GuardrailResult.allow(
                                "first"));

        GuardrailResult blocked = GuardrailResult.block(
                "promptInjection",
                "Potential prompt injection detected");

        when(
                second.evaluate(
                        "attack"))
                .thenReturn(
                        blocked);

        InputGuardrailChain chain = new InputGuardrailChain(
                List.of(
                        first,
                        second,
                        third));

        GuardrailResult result = chain.evaluate(
                "attack");

        assertEquals(
                blocked,
                result);

        verify(
                first)
                .evaluate(
                        "attack");

        verify(
                second)
                .evaluate(
                        "attack");

        verify(
                third,
                never())
                .evaluate(
                        "attack");
    }

    @Test
    void shouldAllowWhenNoGuardrailsRegistered() {

        InputGuardrailChain chain = new InputGuardrailChain(
                List.of());

        GuardrailResult result = chain.evaluate(
                "anything");

        assertTrue(
                result.isAllowed());

        assertEquals(
                "inputGuardrailChain",
                result.guardrailName());
    }

    @Test
    void shouldRejectNullGuardrailList() {

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new InputGuardrailChain(
                        null));

        assertEquals(
                "guardrails must not be null",
                exception.getMessage());
    }

    @Test
    void shouldRejectNullResultReturnedByGuardrail() {

        InputGuardrail guardrail = mock(
                InputGuardrail.class);

        when(
                guardrail.name())
                .thenReturn(
                        "brokenGuardrail");

        when(
                guardrail.evaluate(
                        "input"))
                .thenReturn(
                        null);

        InputGuardrailChain chain = new InputGuardrailChain(
                List.of(
                        guardrail));

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> chain.evaluate(
                        "input"));

        assertEquals(
                "InputGuardrail must not return null: brokenGuardrail",
                exception.getMessage());
    }
}