package com.quince.lawyeraiassistant.security.mcp.result;

import java.util.List;
import java.util.Objects;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Default security service for MCP Tool results.
 *
 * <p>
 * All result policies are evaluated in Spring order.
 * The first DENY terminates evaluation.
 * </p>
 */
@Service
@Profile("mcp-agent")
public final class DefaultMcpToolResultSecurityService
        implements McpToolResultSecurityService {

    private static final String SERVICE_POLICY_NAME = "mcpToolResultSecurityService";

    private final List<McpToolResultSecurityPolicy> policies;

    public DefaultMcpToolResultSecurityService(
            List<McpToolResultSecurityPolicy> policies) {

        Objects.requireNonNull(
                policies,
                "policies must not be null");

        if (policies.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one MCP Tool result security policy is required");
        }

        this.policies = List.copyOf(
                policies);
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

        for (McpToolResultSecurityPolicy policy : policies) {

            McpToolResultSecurityResult result = Objects.requireNonNull(
                    policy.evaluate(
                            normalizedToolName,
                            content),
                    "MCP Tool result security policy must not return null");

            if (result.isDenied()) {
                return result;
            }
        }

        return McpToolResultSecurityResult.allow(
                normalizedToolName,
                SERVICE_POLICY_NAME);
    }
}