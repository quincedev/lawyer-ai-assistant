package com.quince.lawyeraiassistant.security.guardrail.output;

import com.quince.lawyeraiassistant.security.SecurityTest;
import com.quince.lawyeraiassistant.security.guardrail.GuardrailDecision;
import com.quince.lawyeraiassistant.security.guardrail.GuardrailResult;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SecurityTest
class OutputLengthGuardrailTest {

    @Test
    void shouldAllowNormalOutput() {

        OutputLengthGuardrail guardrail = new OutputLengthGuardrail(
                100);

        GuardrailResult result = guardrail.evaluate(
                "根据劳动合同法相关规定，用人单位违法解除劳动合同应承担相应法律责任。");

        assertTrue(
                result.isAllowed());

        assertEquals(
                GuardrailDecision.ALLOW,
                result.decision());

        assertEquals(
                "outputLength",
                result.guardrailName());
    }

    @Test
    void shouldAllowOutputExactlyAtMaximumLength() {

        OutputLengthGuardrail guardrail = new OutputLengthGuardrail(
                10);

        GuardrailResult result = guardrail.evaluate(
                "1234567890");

        assertTrue(
                result.isAllowed());
    }

    @Test
    void shouldBlockOutputExceedingMaximumLength() {

        OutputLengthGuardrail guardrail = new OutputLengthGuardrail(
                10);

        GuardrailResult result = guardrail.evaluate(
                "12345678901");

        assertTrue(
                result.isBlocked());

        assertEquals(
                "outputLength",
                result.guardrailName());

        assertEquals(
                "Output exceeds maximum allowed length",
                result.reason());
    }

    @Test
    void shouldBlockNullOutput() {

        OutputLengthGuardrail guardrail = new OutputLengthGuardrail(
                100);

        GuardrailResult result = guardrail.evaluate(
                null);

        assertTrue(
                result.isBlocked());

        assertEquals(
                "Output must not be blank",
                result.reason());
    }

    @Test
    void shouldBlockEmptyOutput() {

        OutputLengthGuardrail guardrail = new OutputLengthGuardrail(
                100);

        GuardrailResult result = guardrail.evaluate(
                "");

        assertTrue(
                result.isBlocked());
    }

    @Test
    void shouldBlockBlankOutput() {

        OutputLengthGuardrail guardrail = new OutputLengthGuardrail(
                100);

        GuardrailResult result = guardrail.evaluate(
                "   ");

        assertTrue(
                result.isBlocked());

        assertEquals(
                "Output must not be blank",
                result.reason());
    }

    @Test
    void shouldRejectZeroMaximumLength() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new OutputLengthGuardrail(
                        0));

        assertEquals(
                "maxLength must be greater than 0",
                exception.getMessage());
    }

    @Test
    void shouldRejectNegativeMaximumLength() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new OutputLengthGuardrail(
                        -1));

        assertEquals(
                "maxLength must be greater than 0",
                exception.getMessage());
    }
}