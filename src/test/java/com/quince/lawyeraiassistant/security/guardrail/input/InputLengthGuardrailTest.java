package com.quince.lawyeraiassistant.security.guardrail.input;

import com.quince.lawyeraiassistant.security.SecurityTest;
import com.quince.lawyeraiassistant.security.guardrail.GuardrailDecision;
import com.quince.lawyeraiassistant.security.guardrail.GuardrailResult;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SecurityTest
class InputLengthGuardrailTest {

    @Test
    void shouldAllowNormalInput() {

        InputLengthGuardrail guardrail = new InputLengthGuardrail(
                100);

        GuardrailResult result = guardrail.evaluate(
                "研究违法解除劳动合同的法律责任");

        assertTrue(
                result.isAllowed());

        assertEquals(
                GuardrailDecision.ALLOW,
                result.decision());

        assertEquals(
                "inputLength",
                result.guardrailName());
    }

    @Test
    void shouldAllowInputExactlyAtMaximumLength() {

        InputLengthGuardrail guardrail = new InputLengthGuardrail(
                10);

        GuardrailResult result = guardrail.evaluate(
                "1234567890");

        assertTrue(
                result.isAllowed());
    }

    @Test
    void shouldBlockInputExceedingMaximumLength() {

        InputLengthGuardrail guardrail = new InputLengthGuardrail(
                10);

        GuardrailResult result = guardrail.evaluate(
                "12345678901");

        assertTrue(
                result.isBlocked());

        assertEquals(
                "inputLength",
                result.guardrailName());

        assertEquals(
                "Input exceeds maximum allowed length",
                result.reason());
    }

    @Test
    void shouldBlockNullInput() {

        InputLengthGuardrail guardrail = new InputLengthGuardrail(
                100);

        GuardrailResult result = guardrail.evaluate(
                null);

        assertTrue(
                result.isBlocked());

        assertEquals(
                "Input must not be blank",
                result.reason());
    }

    @Test
    void shouldBlockEmptyInput() {

        InputLengthGuardrail guardrail = new InputLengthGuardrail(
                100);

        GuardrailResult result = guardrail.evaluate(
                "");

        assertTrue(
                result.isBlocked());

        assertEquals(
                "Input must not be blank",
                result.reason());
    }

    @Test
    void shouldBlockBlankInput() {

        InputLengthGuardrail guardrail = new InputLengthGuardrail(
                100);

        GuardrailResult result = guardrail.evaluate(
                "   ");

        assertTrue(
                result.isBlocked());

        assertEquals(
                "Input must not be blank",
                result.reason());
    }

    @Test
    void shouldRejectZeroMaximumLength() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new InputLengthGuardrail(
                        0));

        assertEquals(
                "maxLength must be greater than 0",
                exception.getMessage());
    }

    @Test
    void shouldRejectNegativeMaximumLength() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new InputLengthGuardrail(
                        -1));

        assertEquals(
                "maxLength must be greater than 0",
                exception.getMessage());
    }
}