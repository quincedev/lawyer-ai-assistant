package com.quince.lawyeraiassistant.security.authorization.tool.policy;

import java.util.Objects;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.ToolAction;
import com.quince.lawyeraiassistant.agent.tool.AgentToolRegistry;
import com.quince.lawyeraiassistant.security.authorization.tool.ToolAuthorizationPolicy;
import com.quince.lawyeraiassistant.security.authorization.tool.ToolAuthorizationResult;

/**
 * Checks whether the requested Tool actually exists
 * in the current AgentToolRegistry.
 *
 * <p>
 * Tool discovery and Tool authorization are different concerns:
 * this policy only validates capability existence.
 * </p>
 */
@Component
@Order(10)
public final class ToolExistenceAuthorizationPolicy
        implements ToolAuthorizationPolicy {

    private static final String NAME = "toolExistenceAuthorization";

    private final AgentToolRegistry toolRegistry;

    public ToolExistenceAuthorizationPolicy(
            AgentToolRegistry toolRegistry) {

        this.toolRegistry = Objects.requireNonNull(
                toolRegistry,
                "toolRegistry must not be null");
    }

    @Override
    public String name() {

        return NAME;
    }

    @Override
    public ToolAuthorizationResult authorize(
            AgentContext context,
            ToolAction action) {

        Objects.requireNonNull(
                context,
                "AgentContext must not be null");

        Objects.requireNonNull(
                action,
                "ToolAction must not be null");

        String toolName = action.getToolName();

        if (!toolRegistry.contains(
                toolName)) {

            return ToolAuthorizationResult.deny(
                    toolName,
                    NAME,
                    "Tool does not exist");
        }

        return ToolAuthorizationResult.allow(
                toolName,
                NAME);
    }
}