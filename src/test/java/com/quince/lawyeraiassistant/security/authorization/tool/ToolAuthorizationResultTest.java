package com.quince.lawyeraiassistant.security.authorization.tool;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ToolAuthorizationResultTest {

    @Test
    void shouldCreateAllowResult() {

        ToolAuthorizationResult result = ToolAuthorizationResult.allow(
                "searchLegalKnowledge",
                "skillToolAuthorization");

        assertEquals(
                ToolAuthorizationDecision.ALLOW,
                result.decision());

        assertEquals(
                "searchLegalKnowledge",
                result.toolName());

        assertEquals(
                "skillToolAuthorization",
                result.policyName());

        assertEquals(
                "",
                result.reason());

        assertTrue(result.isAllowed());
        assertFalse(result.isDenied());
    }

    @Test
    void shouldCreateDenyResult() {

        ToolAuthorizationResult result = ToolAuthorizationResult.deny(
                "deleteCase",
                "skillToolAuthorization",
                "Tool is not allowed by current Skill");

        assertEquals(
                ToolAuthorizationDecision.DENY,
                result.decision());

        assertTrue(result.isDenied());
        assertFalse(result.isAllowed());

        assertEquals(
                "Tool is not allowed by current Skill",
                result.reason());
    }

    @Test
    void shouldNormalizeToolNameAndPolicyName() {

        ToolAuthorizationResult result = ToolAuthorizationResult.allow(
                "  searchLegalKnowledge  ",
                "  testPolicy  ");

        assertEquals(
                "searchLegalKnowledge",
                result.toolName());

        assertEquals(
                "testPolicy",
                result.policyName());
    }

    @Test
    void shouldNormalizeNullReasonToEmptyString() {

        ToolAuthorizationResult result = new ToolAuthorizationResult(
                ToolAuthorizationDecision.DENY,
                "searchLegalKnowledge",
                "testPolicy",
                null);

        assertEquals(
                "",
                result.reason());
    }

    @Test
    void shouldRejectBlankToolName() {

        assertThrows(
                IllegalArgumentException.class,
                () -> ToolAuthorizationResult.allow(
                        "   ",
                        "testPolicy"));
    }

    @Test
    void shouldRejectBlankPolicyName() {

        assertThrows(
                IllegalArgumentException.class,
                () -> ToolAuthorizationResult.allow(
                        "searchLegalKnowledge",
                        "   "));
    }

    @Test
    void shouldRejectNullDecision() {

        assertThrows(
                NullPointerException.class,
                () -> new ToolAuthorizationResult(
                        null,
                        "searchLegalKnowledge",
                        "testPolicy",
                        ""));
    }
}