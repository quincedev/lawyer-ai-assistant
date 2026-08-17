package com.quince.lawyeraiassistant.security.mcp.policy;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.quince.lawyeraiassistant.security.mcp.McpToolSecurityResult;
import com.quince.lawyeraiassistant.security.mcp.config.McpToolSecurityProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.quince.lawyeraiassistant.security.SecurityTest;

@SecurityTest
class McpToolArgumentSecurityPolicyTest {

        private static final String TOOL_NAME = "searchLegalKnowledge";

        private static final String POLICY_NAME = "mcpToolArguments";

        @Test
        void shouldAllowValidLegalQuestion() {

                McpToolArgumentSecurityPolicy policy = createPolicy(
                                100,
                                true);

                McpToolSecurityResult result = policy.evaluate(
                                TOOL_NAME,
                                Map.of(
                                                "legalQuestion",
                                                "What remedies are available for breach of contract?"));

                assertTrue(result.isAllowed());
                assertEquals(TOOL_NAME, result.toolName());
                assertEquals(POLICY_NAME, result.policyName());
        }

        @Test
        void shouldDenyMissingLegalQuestion() {

                McpToolSecurityResult result = createPolicy(100, true).evaluate(
                                TOOL_NAME,
                                Map.of());

                assertDenied(
                                result,
                                "Required MCP Tool argument is missing: legalQuestion");
        }

        @Test
        void shouldDenyNonStringLegalQuestion() {

                McpToolSecurityResult result = createPolicy(100, true).evaluate(
                                TOOL_NAME,
                                Map.of(
                                                "legalQuestion",
                                                42));

                assertDenied(
                                result,
                                "MCP Tool argument must be a String: legalQuestion");
        }

        @Test
        void shouldDenyBlankLegalQuestion() {

                McpToolSecurityResult result = createPolicy(100, true).evaluate(
                                TOOL_NAME,
                                Map.of(
                                                "legalQuestion",
                                                "   "));

                assertDenied(
                                result,
                                "MCP Tool argument must not be blank: legalQuestion");
        }

        @Test
        void shouldDenyOversizedLegalQuestion() {

                McpToolSecurityResult result = createPolicy(5, true).evaluate(
                                TOOL_NAME,
                                Map.of(
                                                "legalQuestion",
                                                "123456"));

                assertDenied(
                                result,
                                "MCP Tool argument exceeds maximum allowed length: legalQuestion");
        }

        @Test
        void shouldAllowQuestionExactlyAtMaximumLength() {

                McpToolSecurityResult result = createPolicy(5, true).evaluate(
                                TOOL_NAME,
                                Map.of(
                                                "legalQuestion",
                                                "12345"));

                assertTrue(result.isAllowed());
                assertEquals(TOOL_NAME, result.toolName());
                assertEquals(POLICY_NAME, result.policyName());
        }

        @Test
        void shouldDenyUnknownArgument() {

                McpToolSecurityResult result = createPolicy(100, true).evaluate(
                                TOOL_NAME,
                                Map.of(
                                                "legalQuestion",
                                                "Is this clause enforceable?",
                                                "untrustedField",
                                                "value"));

                assertDenied(
                                result,
                                "Unknown MCP Tool argument: untrustedField");
        }

        @Test
        void shouldDenyToolWithoutArgumentSchema() {

                McpToolSecurityResult result = createPolicy(100, true).evaluate(
                                "deleteCase",
                                Map.of());

                assertDenied(
                                result,
                                "No MCP Tool argument schema is configured: deleteCase");

                assertEquals("deleteCase", result.toolName());
        }

        private McpToolArgumentSecurityPolicy createPolicy(
                        int maxStringLength,
                        boolean rejectUnknownFields) {

                McpToolSecurityProperties properties = new McpToolSecurityProperties();

                properties.getArguments().setMaxStringLength(
                                maxStringLength);

                properties.getArguments().setRejectUnknownFields(
                                rejectUnknownFields);

                return new McpToolArgumentSecurityPolicy(
                                properties);
        }

        private void assertDenied(
                        McpToolSecurityResult result,
                        String reason) {

                assertTrue(result.isDenied());
                assertEquals(POLICY_NAME, result.policyName());
                assertEquals(reason, result.reason());
        }
}
