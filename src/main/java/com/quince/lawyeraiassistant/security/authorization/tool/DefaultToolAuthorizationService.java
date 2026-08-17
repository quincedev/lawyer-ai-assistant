package com.quince.lawyeraiassistant.security.authorization.tool;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.ToolAction;

@Service
public final class DefaultToolAuthorizationService
        implements ToolAuthorizationService {

    private static final String NAME = "toolAuthorization";

    private final List<ToolAuthorizationPolicy> policies;

    public DefaultToolAuthorizationService(
            List<ToolAuthorizationPolicy> policies) {

        Objects.requireNonNull(
                policies,
                "policies must not be null");

        if (policies.isEmpty()) {

            throw new IllegalArgumentException(
                    "At least one ToolAuthorizationPolicy is required");
        }

        this.policies = List.copyOf(
                policies);
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

        for (ToolAuthorizationPolicy policy : policies) {

            ToolAuthorizationResult result = Objects.requireNonNull(
                    policy.authorize(
                            context,
                            action),
                    "ToolAuthorizationPolicy must not return null: "
                            + policy.name());

            if (result.isDenied()) {

                return result;
            }
        }

        return ToolAuthorizationResult.allow(
                action.getToolName(),
                NAME);
    }
}