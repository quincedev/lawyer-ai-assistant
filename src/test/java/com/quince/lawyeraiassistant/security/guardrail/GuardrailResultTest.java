package com.quince.lawyeraiassistant.security.guardrail;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuardrailResultTest {

    @Test
    void shouldCreateAllowResult() {

        GuardrailResult result = GuardrailResult.allow(
                "inputLength");

        assertEquals(
                GuardrailDecision.ALLOW,
                result.decision());

        assertEquals(
                "inputLength",
                result.guardrailName());

        assertEquals(
                "",
                result.reason());

        assertTrue(
                result.isAllowed());

        assertFalse(
                result.isBlocked());
    }

    @Test
    void shouldCreateBlockResult() {

        GuardrailResult result = GuardrailResult.block(
                "promptInjection",
                "Potential prompt injection detected");

        assertEquals(
                GuardrailDecision.BLOCK,
                result.decision());

        assertEquals(
                "promptInjection",
                result.guardrailName());

        assertEquals(
                "Potential prompt injection detected",
                result.reason());

        assertFalse(
                result.isAllowed());

        assertTrue(
                result.isBlocked());
    }

    @Test
    void shouldNormalizeNullReasonToEmptyString() {

        GuardrailResult result = new GuardrailResult(
                GuardrailDecision.BLOCK,
                "testGuardrail",
                null);

        assertEquals(
                "",
                result.reason());
    }

    @Test
    void shouldRejectNullDecision() {

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new GuardrailResult(
                        null,
                        "testGuardrail",
                        ""));

        assertEquals(
                "decision must not be null",
                exception.getMessage());
    }

    @Test
    void shouldRejectNullGuardrailName() {

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new GuardrailResult(
                        GuardrailDecision.ALLOW,
                        null,
                        ""));

        assertEquals(
                "guardrailName must not be null",
                exception.getMessage());
    }
}