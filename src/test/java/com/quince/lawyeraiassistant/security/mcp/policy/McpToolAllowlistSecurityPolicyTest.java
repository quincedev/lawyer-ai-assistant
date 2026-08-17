package com.quince.lawyeraiassistant.security.mcp.policy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.quince.lawyeraiassistant.security.mcp.McpToolSecurityResult;
import com.quince.lawyeraiassistant.security.mcp.config.McpToolSecurityProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.quince.lawyeraiassistant.security.SecurityTest;

@SecurityTest
class McpToolAllowlistSecurityPolicyTest {

        @Test
        void shouldAllowExplicitlyAllowedTool() {

                McpToolSecurityProperties properties = new McpToolSecurityProperties();

                properties.setAllowedTools(
                                List.of(
                                                "searchLegalKnowledge"));

                McpToolAllowlistSecurityPolicy policy = new McpToolAllowlistSecurityPolicy(
                                properties);

                McpToolSecurityResult result = policy.evaluate(
                                "searchLegalKnowledge",
                                Map.of());

                assertTrue(
                                result.isAllowed());

                assertEquals(
                                "mcpToolAllowlist",
                                result.policyName());
        }

        @Test
        void shouldDenyToolNotInAllowlist() {

                McpToolSecurityProperties properties = new McpToolSecurityProperties();

                properties.setAllowedTools(
                                List.of(
                                                "searchLegalKnowledge"));

                McpToolAllowlistSecurityPolicy policy = new McpToolAllowlistSecurityPolicy(
                                properties);

                McpToolSecurityResult result = policy.evaluate(
                                "deleteCase",
                                Map.of());

                assertTrue(
                                result.isDenied());

                assertEquals(
                                "MCP Tool is not allowed by server policy",
                                result.reason());
        }

        @Test
        void shouldDenyAllToolsWhenAllowlistIsEmpty() {

                McpToolSecurityProperties properties = new McpToolSecurityProperties();

                McpToolAllowlistSecurityPolicy policy = new McpToolAllowlistSecurityPolicy(
                                properties);

                McpToolSecurityResult result = policy.evaluate(
                                "searchLegalKnowledge",
                                Map.of());

                assertTrue(
                                result.isDenied());
        }

        @Test
        void shouldNormalizeConfiguredToolWhitespace() {

                McpToolSecurityProperties properties = new McpToolSecurityProperties();

                properties.setAllowedTools(
                                List.of(
                                                "  searchLegalKnowledge  "));

                McpToolAllowlistSecurityPolicy policy = new McpToolAllowlistSecurityPolicy(
                                properties);

                McpToolSecurityResult result = policy.evaluate(
                                "searchLegalKnowledge",
                                Map.of());

                assertTrue(
                                result.isAllowed());
        }

        @Test
        void shouldDenyBlankToolName() {

                McpToolSecurityProperties properties = new McpToolSecurityProperties();

                properties.setAllowedTools(
                                List.of(
                                                "searchLegalKnowledge"));

                McpToolAllowlistSecurityPolicy policy = new McpToolAllowlistSecurityPolicy(
                                properties);

                McpToolSecurityResult result = policy.evaluate(
                                "   ",
                                Map.of());

                assertTrue(
                                result.isDenied());

                assertEquals(
                                "MCP Tool name must not be blank",
                                result.reason());
        }
}