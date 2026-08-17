package com.quince.lawyeraiassistant.security.guardrail.output;

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
class OutputGuardrailChainTest {

    @Test
    void shouldAllowWhenAllGuardrailsAllow() {

        OutputGuardrail first = mock(
                OutputGuardrail.class);

        OutputGuardrail second = mock(
                OutputGuardrail.class);

        String output = "正常法律分析结果";

        when(
                first.evaluate(
                        output))
                .thenReturn(
                        GuardrailResult.allow(
                                "first"));

        when(
                second.evaluate(
                        output))
                .thenReturn(
                        GuardrailResult.allow(
                                "second"));

        OutputGuardrailChain chain = new OutputGuardrailChain(
                List.of(
                        first,
                        second));

        GuardrailResult result = chain.evaluate(
                output);

        assertTrue(
                result.isAllowed());

        assertEquals(
                "outputGuardrailChain",
                result.guardrailName());

        verify(
                first)
                .evaluate(
                        output);

        verify(
                second)
                .evaluate(
                        output);
    }

    @Test
    void shouldReturnFirstBlockedResult() {

        OutputGuardrail first = mock(
                OutputGuardrail.class);

        OutputGuardrail second = mock(
                OutputGuardrail.class);

        String output = "unsafe output";

        GuardrailResult blocked = GuardrailResult.block(
                "outputLength",
                "Output exceeds maximum allowed length");

        when(
                first.evaluate(
                        output))
                .thenReturn(
                        blocked);

        OutputGuardrailChain chain = new OutputGuardrailChain(
                List.of(
                        first,
                        second));

        GuardrailResult result = chain.evaluate(
                output);

        assertEquals(
                blocked,
                result);

        verify(
                first)
                .evaluate(
                        output);

        verify(
                second,
                never())
                .evaluate(
                        output);
    }

    @Test
    void shouldStopWhenLaterGuardrailBlocks() {

        OutputGuardrail first = mock(
                OutputGuardrail.class);

        OutputGuardrail second = mock(
                OutputGuardrail.class);

        OutputGuardrail third = mock(
                OutputGuardrail.class);

        String output = "DEEPSEEK_API_KEY=abcdefgh123456";

        when(
                first.evaluate(
                        output))
                .thenReturn(
                        GuardrailResult.allow(
                                "outputLength"));

        GuardrailResult blocked = GuardrailResult.block(
                "sensitiveOutput",
                "Potential sensitive output detected");

        when(
                second.evaluate(
                        output))
                .thenReturn(
                        blocked);

        OutputGuardrailChain chain = new OutputGuardrailChain(
                List.of(
                        first,
                        second,
                        third));

        GuardrailResult result = chain.evaluate(
                output);

        assertEquals(
                blocked,
                result);

        verify(
                first)
                .evaluate(
                        output);

        verify(
                second)
                .evaluate(
                        output);

        verify(
                third,
                never())
                .evaluate(
                        output);
    }

    @Test
    void shouldAllowWhenNoGuardrailsRegistered() {

        OutputGuardrailChain chain = new OutputGuardrailChain(
                List.of());

        GuardrailResult result = chain.evaluate(
                "normal output");

        assertTrue(
                result.isAllowed());

        assertEquals(
                "outputGuardrailChain",
                result.guardrailName());
    }

    @Test
    void shouldRejectNullGuardrailList() {

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new OutputGuardrailChain(
                        null));

        assertEquals(
                "guardrails must not be null",
                exception.getMessage());
    }

    @Test
    void shouldRejectNullResultReturnedByGuardrail() {

        OutputGuardrail guardrail = mock(
                OutputGuardrail.class);

        when(
                guardrail.name())
                .thenReturn(
                        "brokenOutputGuardrail");

        when(
                guardrail.evaluate(
                        "output"))
                .thenReturn(
                        null);

        OutputGuardrailChain chain = new OutputGuardrailChain(
                List.of(
                        guardrail));

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> chain.evaluate(
                        "output"));

        assertEquals(
                "OutputGuardrail must not return null: brokenOutputGuardrail",
                exception.getMessage());
    }
}