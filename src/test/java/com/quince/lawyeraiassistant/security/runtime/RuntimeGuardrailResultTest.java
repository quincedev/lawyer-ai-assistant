package com.quince.lawyeraiassistant.security.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RuntimeGuardrailResultTest {

    @Test
    void shouldCreateAllowResult() {

        RuntimeGuardrailResult result = RuntimeGuardrailResult.allow(
                "stepLimit");

        assertTrue(
                result.isAllowed());

        assertFalse(
                result.isDenied());

        assertEquals(
                RuntimeGuardrailDecision.ALLOW,
                result.decision());

        assertEquals(
                "stepLimit",
                result.policyName());

        assertEquals(
                "",
                result.reason());
    }

    @Test
    void shouldCreateDenyResult() {

        RuntimeGuardrailResult result = RuntimeGuardrailResult.deny(
                "stepLimit",
                "Maximum Agent execution steps exceeded");

        assertTrue(
                result.isDenied());

        assertFalse(
                result.isAllowed());

        assertEquals(
                RuntimeGuardrailDecision.DENY,
                result.decision());

        assertEquals(
                "stepLimit",
                result.policyName());

        assertEquals(
                "Maximum Agent execution steps exceeded",
                result.reason());
    }

    @Test
    void shouldNormalizePolicyNameAndReason() {

        RuntimeGuardrailResult result = RuntimeGuardrailResult.deny(
                "  stepLimit  ",
                "  limit exceeded  ");

        assertEquals(
                "stepLimit",
                result.policyName());

        assertEquals(
                "limit exceeded",
                result.reason());
    }

    @Test
    void shouldNormalizeNullReasonToEmptyString() {

        RuntimeGuardrailResult result = RuntimeGuardrailResult.deny(
                "stepLimit",
                null);

        assertEquals(
                "",
                result.reason());
    }

    @Test
    void shouldRejectNullDecision() {

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new RuntimeGuardrailResult(
                        null,
                        "stepLimit",
                        ""));

        assertEquals(
                "decision must not be null",
                exception.getMessage());
    }

    @Test
    void shouldRejectNullPolicyName() {

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> RuntimeGuardrailResult.allow(
                        null));

        assertEquals(
                "policyName must not be null",
                exception.getMessage());
    }

    @Test
    void shouldRejectBlankPolicyName() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> RuntimeGuardrailResult.allow(
                        "   "));

        assertEquals(
                "policyName must not be blank",
                exception.getMessage());
    }
}