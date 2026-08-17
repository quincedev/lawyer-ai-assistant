package com.quince.lawyeraiassistant.security.mcp.result.policy;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.quince.lawyeraiassistant.security.mcp.result.McpToolResultSecurityPolicy;
import com.quince.lawyeraiassistant.security.mcp.result.McpToolResultSecurityResult;

/**
 * Detects common indirect prompt injection signals
 * inside data returned by MCP Tools.
 *
 * <p>
 * MCP Tool results are treated as untrusted data.
 * They must not be allowed to redefine Agent instructions,
 * request privilege escalation, or trigger additional Tool
 * execution.
 * </p>
 */
@Component
@Profile("mcp-agent")
@Order(10)
public final class McpIndirectPromptInjectionPolicy
        implements McpToolResultSecurityPolicy {

    private static final String NAME = "mcpIndirectPromptInjection";

    private static final int DENY_THRESHOLD = 2;

    private static final List<String> INSTRUCTION_OVERRIDE_SIGNALS = List.of(
            "ignore previous instructions",
            "ignore all previous instructions",
            "disregard previous instructions",
            "forget previous instructions",
            "override previous instructions",
            "ignore system instructions");

    private static final List<String> PRIVILEGE_ESCALATION_SIGNALS = List.of(
            "you are now system",
            "you are now administrator",
            "act as system",
            "act as administrator",
            "developer mode",
            "system administrator");

    private static final List<String> SECRET_EXTRACTION_SIGNALS = List.of(
            "reveal system prompt",
            "show system prompt",
            "print system prompt",
            "reveal your instructions",
            "show your instructions",
            "reveal secrets");

    private static final List<String> TOOL_CONTROL_SIGNALS = List.of(
            "call tool",
            "call the tool",
            "invoke tool",
            "invoke the tool",
            "execute tool",
            "use tool",
            "call delete",
            "send data to");

    @Override
    public String name() {

        return NAME;
    }

    @Override
    public McpToolResultSecurityResult evaluate(
            String toolName,
            String content) {

        Objects.requireNonNull(
                toolName,
                "toolName must not be null");

        Objects.requireNonNull(
                content,
                "content must not be null");

        String normalizedToolName = toolName.strip();

        if (normalizedToolName.isEmpty()) {
            throw new IllegalArgumentException(
                    "toolName must not be blank");
        }

        if (content.isBlank()) {

            return McpToolResultSecurityResult.allow(
                    normalizedToolName,
                    NAME);
        }

        String normalizedContent = content.toLowerCase(
                Locale.ROOT);

        int score = calculateScore(
                normalizedContent);

        if (score >= DENY_THRESHOLD) {

            return McpToolResultSecurityResult.deny(
                    normalizedToolName,
                    NAME,
                    "Potential indirect prompt injection detected in MCP Tool result");
        }

        return McpToolResultSecurityResult.allow(
                normalizedToolName,
                NAME);
    }

    private int calculateScore(
            String content) {

        int score = 0;

        if (containsAny(
                content,
                INSTRUCTION_OVERRIDE_SIGNALS)) {

            score++;
        }

        if (containsAny(
                content,
                PRIVILEGE_ESCALATION_SIGNALS)) {

            score++;
        }

        if (containsAny(
                content,
                SECRET_EXTRACTION_SIGNALS)) {

            score++;
        }

        if (containsAny(
                content,
                TOOL_CONTROL_SIGNALS)) {

            score++;
        }

        return score;
    }

    private boolean containsAny(
            String content,
            List<String> signals) {

        return signals.stream()
                .anyMatch(
                        content::contains);
    }
}