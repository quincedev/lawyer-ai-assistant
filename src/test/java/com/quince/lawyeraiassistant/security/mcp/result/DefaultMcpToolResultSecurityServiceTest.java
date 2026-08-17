package com.quince.lawyeraiassistant.security.mcp.result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import com.quince.lawyeraiassistant.security.SecurityTest;

@SecurityTest
class DefaultMcpToolResultSecurityServiceTest {

    @Test
    void shouldAllowWhenAllPoliciesAllow() {

        McpToolResultSecurityPolicy first = mock(McpToolResultSecurityPolicy.class);

        McpToolResultSecurityPolicy second = mock(McpToolResultSecurityPolicy.class);

        when(first.evaluate(
                "searchLegalKnowledge",
                "legal result"))
                .thenReturn(
                        McpToolResultSecurityResult.allow(
                                "searchLegalKnowledge",
                                "first"));

        when(second.evaluate(
                "searchLegalKnowledge",
                "legal result"))
                .thenReturn(
                        McpToolResultSecurityResult.allow(
                                "searchLegalKnowledge",
                                "second"));

        DefaultMcpToolResultSecurityService service = new DefaultMcpToolResultSecurityService(
                List.of(first, second));

        McpToolResultSecurityResult result = service.evaluate(
                "searchLegalKnowledge",
                "legal result");

        assertTrue(result.isAllowed());

        assertEquals(
                "mcpToolResultSecurityService",
                result.policyName());
    }

    @Test
    void shouldStopAtFirstDeny() {

        McpToolResultSecurityPolicy first = mock(McpToolResultSecurityPolicy.class);

        McpToolResultSecurityPolicy second = mock(McpToolResultSecurityPolicy.class);

        when(first.evaluate(
                "searchLegalKnowledge",
                "malicious result"))
                .thenReturn(
                        McpToolResultSecurityResult.deny(
                                "searchLegalKnowledge",
                                "first",
                                "Indirect prompt injection"));

        DefaultMcpToolResultSecurityService service = new DefaultMcpToolResultSecurityService(
                List.of(first, second));

        McpToolResultSecurityResult result = service.evaluate(
                "searchLegalKnowledge",
                "malicious result");

        assertTrue(result.isDenied());

        verify(
                second,
                never())
                .evaluate(
                        "searchLegalKnowledge",
                        "malicious result");
    }

    @Test
    void shouldRejectEmptyPolicyList() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new DefaultMcpToolResultSecurityService(
                        List.of()));
    }
}