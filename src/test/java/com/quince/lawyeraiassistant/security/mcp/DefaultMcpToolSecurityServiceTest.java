package com.quince.lawyeraiassistant.security.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import com.quince.lawyeraiassistant.security.SecurityTest;

@SecurityTest
class DefaultMcpToolSecurityServiceTest {

    @Test
    void shouldAllowWhenAllPoliciesAllow() {

        McpToolSecurityPolicy first = mock(McpToolSecurityPolicy.class);

        McpToolSecurityPolicy second = mock(McpToolSecurityPolicy.class);

        when(first.evaluate(
                "searchLegalKnowledge",
                Map.of("legalQuestion", "劳动合同")))
                .thenReturn(
                        McpToolSecurityResult.allow(
                                "searchLegalKnowledge",
                                "first"));

        when(second.evaluate(
                "searchLegalKnowledge",
                Map.of("legalQuestion", "劳动合同")))
                .thenReturn(
                        McpToolSecurityResult.allow(
                                "searchLegalKnowledge",
                                "second"));

        DefaultMcpToolSecurityService service = new DefaultMcpToolSecurityService(
                List.of(first, second));

        McpToolSecurityResult result = service.evaluate(
                "searchLegalKnowledge",
                Map.of(
                        "legalQuestion",
                        "劳动合同"));

        assertTrue(result.isAllowed());

        assertEquals(
                "mcpToolSecurityService",
                result.policyName());
    }

    @Test
    void shouldStopAtFirstDeny() {

        McpToolSecurityPolicy first = mock(McpToolSecurityPolicy.class);

        McpToolSecurityPolicy second = mock(McpToolSecurityPolicy.class);

        Map<String, Object> arguments = Map.of(
                "legalQuestion",
                "劳动合同");

        when(first.evaluate(
                "searchLegalKnowledge",
                arguments))
                .thenReturn(
                        McpToolSecurityResult.deny(
                                "searchLegalKnowledge",
                                "first",
                                "Denied"));

        DefaultMcpToolSecurityService service = new DefaultMcpToolSecurityService(
                List.of(first, second));

        McpToolSecurityResult result = service.evaluate(
                "searchLegalKnowledge",
                arguments);

        assertTrue(result.isDenied());

        assertEquals(
                "first",
                result.policyName());

        verify(
                second,
                never())
                .evaluate(
                        "searchLegalKnowledge",
                        arguments);
    }

    @Test
    void shouldRejectEmptyPolicyList() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new DefaultMcpToolSecurityService(
                        List.of()));
    }
}