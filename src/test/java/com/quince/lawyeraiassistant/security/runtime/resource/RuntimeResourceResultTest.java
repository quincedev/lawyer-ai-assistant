package com.quince.lawyeraiassistant.security.runtime.resource;

import com.quince.lawyeraiassistant.security.SecurityTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SecurityTest
class RuntimeResourceResultTest {

    @Test
    void shouldCreateAllowResult() {

        RuntimeResourceResult result = RuntimeResourceResult.allow(
                "observationLengthLimit");

        assertTrue(
                result.isAllowed());

        assertFalse(
                result.isDenied());

        assertEquals(
                RuntimeResourceDecision.ALLOW,
                result.decision());

        assertEquals(
                "observationLengthLimit",
                result.policyName());

        assertEquals(
                "",
                result.reason());
    }

    @Test
    void shouldCreateDenyResult() {

        RuntimeResourceResult result = RuntimeResourceResult.deny(
                "contextLengthLimit",
                "Maximum Agent context length exceeded");

        assertTrue(
                result.isDenied());

        assertFalse(
                result.isAllowed());

        assertEquals(
                RuntimeResourceDecision.DENY,
                result.decision());

        assertEquals(
                "Maximum Agent context length exceeded",
                result.reason());
    }

    @Test
    void shouldNormalizePolicyAndReason() {

        RuntimeResourceResult result = RuntimeResourceResult.deny(
                "  contextLengthLimit  ",
                "  exceeded  ");

        assertEquals(
                "contextLengthLimit",
                result.policyName());

        assertEquals(
                "exceeded",
                result.reason());
    }

    @Test
    void shouldRejectBlankPolicyName() {

        assertThrows(
                IllegalArgumentException.class,
                () -> RuntimeResourceResult.allow(
                        "   "));
    }

    @Test
    void shouldRejectNullDecision() {

        assertThrows(
                NullPointerException.class,
                () -> new RuntimeResourceResult(
                        null,
                        "testPolicy",
                        ""));
    }
}
