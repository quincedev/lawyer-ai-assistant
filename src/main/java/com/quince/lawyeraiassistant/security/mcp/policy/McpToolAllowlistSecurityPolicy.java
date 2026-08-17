package com.quince.lawyeraiassistant.security.mcp.policy;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.quince.lawyeraiassistant.security.mcp.McpToolSecurityPolicy;
import com.quince.lawyeraiassistant.security.mcp.McpToolSecurityResult;
import com.quince.lawyeraiassistant.security.mcp.config.McpToolSecurityProperties;

/**
 * Server-side allowlist policy for MCP Tool invocations.
 *
 * <p>
 * Only Tools explicitly configured in:
 * </p>
 *
 * <pre>
 * app.mcp.security.allowed - tools
 * </pre>
 *
 * <p>
 * are allowed to proceed.
 * </p>
 *
 * <p>
 * Missing or empty configuration means no Tool is allowed.
 * This policy therefore follows a fail-closed model.
 * </p>
 */
@Component
@Profile("mcp-server")
@Order(10)
public final class McpToolAllowlistSecurityPolicy
        implements McpToolSecurityPolicy {

    private static final String NAME = "mcpToolAllowlist";

    private static final String BLANK_TOOL_NAME = "<blank>";

    private final Set<String> allowedTools;

    public McpToolAllowlistSecurityPolicy(
            McpToolSecurityProperties properties) {

        Objects.requireNonNull(
                properties,
                "properties must not be null");

        Set<String> normalized = new LinkedHashSet<>();

        for (String toolName : properties.getAllowedTools()) {

            if (toolName == null
                    || toolName.isBlank()) {

                continue;
            }

            normalized.add(
                    normalize(
                            toolName));
        }

        this.allowedTools = Collections.unmodifiableSet(
                normalized);
    }

    @Override
    public String name() {

        return NAME;
    }

    @Override
    public McpToolSecurityResult evaluate(
            String toolName,
            Map<String, Object> arguments) {

        Objects.requireNonNull(
                toolName,
                "toolName must not be null");

        Objects.requireNonNull(
                arguments,
                "arguments must not be null");

        String normalizedToolName = normalize(
                toolName);

        if (normalizedToolName.isEmpty()) {

            return McpToolSecurityResult.deny(
                    BLANK_TOOL_NAME,
                    NAME,
                    "MCP Tool name must not be blank");
        }

        if (!allowedTools.contains(
                normalizedToolName)) {

            return McpToolSecurityResult.deny(
                    toolName,
                    NAME,
                    "MCP Tool is not allowed by server policy");
        }

        return McpToolSecurityResult.allow(
                toolName,
                NAME);
    }

    private String normalize(
            String toolName) {

        return toolName.strip();
    }
}
