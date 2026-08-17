package com.quince.lawyeraiassistant.security.mcp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import com.quince.lawyeraiassistant.security.SecurityTest;

@SecurityTest
class McpToolSecurityResultTest {

    @Test
    void shouldCreateAllowResult() {

        McpToolSecurityResult result = McpToolSecurityResult.allow(
                "searchLegalKnowledge",
                "testPolicy");

        assertTrue(result.isAllowed());
        assertFalse(result.isDenied());

        assertEquals(
                McpToolSecurityDecision.ALLOW,
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

        McpToolSecurityResult result = McpToolSecurityResult.deny(
                "deleteCase",
                "testPolicy",
                "Tool is not allowed");

        assertTrue(result.isDenied());
        assertFalse(result.isAllowed());

        assertEquals(
                "Tool is not allowed",
                result.reason());
    }

    @Test
    void shouldNormalizeValues() {

        McpToolSecurityResult result = McpToolSecurityResult.deny(
                "  deleteCase  ",
                "  testPolicy  ",
                "  denied  ");

        assertEquals(
                "deleteCase",
                result.toolName());

        assertEquals(
                "testPolicy",
                result.policyName());

        assertEquals(
                "denied",
                result.reason());
    }

    @Test
    void shouldRejectBlankToolName() {

        assertThrows(
                IllegalArgumentException.class,
                () -> McpToolSecurityResult.allow(
                        "   ",
                        "testPolicy"));
    }

    @Test
    void shouldRejectBlankPolicyName() {

        assertThrows(
                IllegalArgumentException.class,
                () -> McpToolSecurityResult.allow(
                        "searchLegalKnowledge",
                        "   "));
    }
}