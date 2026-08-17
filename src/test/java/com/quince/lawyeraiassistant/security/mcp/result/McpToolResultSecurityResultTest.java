package com.quince.lawyeraiassistant.security.mcp.result;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpToolResultSecurityResultTest {

    @Test
    void shouldCreateAllowResult() {

        McpToolResultSecurityResult result = McpToolResultSecurityResult.allow(
                "searchLegalKnowledge",
                "testPolicy");

        assertTrue(
                result.isAllowed());

        assertFalse(
                result.isDenied());

        assertEquals(
                McpToolResultSecurityDecision.ALLOW,
                result.decision());

        assertEquals(
                "searchLegalKnowledge",
                result.toolName());

        assertEquals(
                "testPolicy",
                result.policyName());

        assertEquals(
                "",
                result.reason());
    }

    @Test
    void shouldCreateDenyResult() {

        McpToolResultSecurityResult result = McpToolResultSecurityResult.deny(
                "searchLegalKnowledge",
                "mcpIndirectPromptInjection",
                "Potential indirect prompt injection detected");

        assertTrue(
                result.isDenied());

        assertFalse(
                result.isAllowed());

        assertEquals(
                McpToolResultSecurityDecision.DENY,
                result.decision());

        assertEquals(
                "Potential indirect prompt injection detected",
                result.reason());
    }

    @Test
    void shouldNormalizeValues() {

        McpToolResultSecurityResult result = McpToolResultSecurityResult.deny(
                "  searchLegalKnowledge  ",
                "  testPolicy  ",
                "  denied  ");

        assertEquals(
                "searchLegalKnowledge",
                result.toolName());

        assertEquals(
                "testPolicy",
                result.policyName());

        assertEquals(
                "denied",
                result.reason());
    }

    @Test
    void shouldNormalizeNullReasonToEmptyString() {

        McpToolResultSecurityResult result = new McpToolResultSecurityResult(
                McpToolResultSecurityDecision.DENY,
                "searchLegalKnowledge",
                "testPolicy",
                null);

        assertEquals(
                "",
                result.reason());
    }

    @Test
    void shouldRejectNullDecision() {

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new McpToolResultSecurityResult(
                        null,
                        "searchLegalKnowledge",
                        "testPolicy",
                        ""));

        assertEquals(
                "decision must not be null",
                exception.getMessage());
    }

    @Test
    void shouldRejectNullToolName() {

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> McpToolResultSecurityResult.allow(
                        null,
                        "testPolicy"));

        assertEquals(
                "toolName must not be null",
                exception.getMessage());
    }

    @Test
    void shouldRejectBlankToolName() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> McpToolResultSecurityResult.allow(
                        "   ",
                        "testPolicy"));

        assertEquals(
                "toolName must not be blank",
                exception.getMessage());
    }

    @Test
    void shouldRejectNullPolicyName() {

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> McpToolResultSecurityResult.allow(
                        "searchLegalKnowledge",
                        null));

        assertEquals(
                "policyName must not be null",
                exception.getMessage());
    }

    @Test
    void shouldRejectBlankPolicyName() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> McpToolResultSecurityResult.allow(
                        "searchLegalKnowledge",
                        "   "));

        assertEquals(
                "policyName must not be blank",
                exception.getMessage());
    }
}