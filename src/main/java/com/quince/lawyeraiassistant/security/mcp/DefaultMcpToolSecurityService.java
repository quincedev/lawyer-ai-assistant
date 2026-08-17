package com.quince.lawyeraiassistant.security.mcp;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Default implementation of MCP Server-side Tool security.
 *
 * <p>
 * Policies are evaluated in Spring order.
 * The first DENY terminates evaluation.
 * </p>
 *
 * <p>
 * No configured policies is considered a security
 * configuration error and therefore fails closed.
 * </p>
 */
@Profile("mcp-server")
@Service
public final class DefaultMcpToolSecurityService
        implements McpToolSecurityService {

    private static final String SERVICE_POLICY_NAME = "mcpToolSecurityService";

    private final List<McpToolSecurityPolicy> policies;

    public DefaultMcpToolSecurityService(
            List<McpToolSecurityPolicy> policies) {

        Objects.requireNonNull(
                policies,
                "policies must not be null");

        if (policies.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one MCP Tool security policy is required");
        }

        this.policies = List.copyOf(
                policies);
    }

    @Override
    public McpToolSecurityResult evaluate(
            String toolName,
            Map<String, Object> arguments) {

        Objects.requireNonNull(
                toolName,
                "toolName must not be null");

        if (toolName.isBlank()) {
            throw new IllegalArgumentException(
                    "toolName must not be blank");
        }

        Map<String, Object> safeArguments = arguments == null
                ? Map.of()
                : Map.copyOf(arguments);

        String normalizedToolName = toolName.strip();

        for (McpToolSecurityPolicy policy : policies) {

            McpToolSecurityResult result = Objects.requireNonNull(
                    policy.evaluate(
                            normalizedToolName,
                            safeArguments),
                    "MCP Tool security policy must not return null");

            if (result.isDenied()) {
                return result;
            }
        }

        return McpToolSecurityResult.allow(
                normalizedToolName,
                SERVICE_POLICY_NAME);
    }
}