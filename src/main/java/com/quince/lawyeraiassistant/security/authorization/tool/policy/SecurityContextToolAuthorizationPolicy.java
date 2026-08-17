package com.quince.lawyeraiassistant.security.authorization.tool.policy;

import java.util.Objects;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.ToolAction;
import com.quince.lawyeraiassistant.security.authorization.tool.ToolAuthorizationPolicy;
import com.quince.lawyeraiassistant.security.authorization.tool.ToolAuthorizationResult;

@Component
@Order(5)
public final class SecurityContextToolAuthorizationPolicy
        implements ToolAuthorizationPolicy {

    private static final String NAME = "securityContextToolAuthorization";

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

        if (!context.hasLegalSecurityContext()) {

            return ToolAuthorizationResult.deny(
                    action.getToolName(),
                    NAME,
                    "Tool authorization denied because LegalSecurityContext is missing");
        }

        return ToolAuthorizationResult.allow(
                action.getToolName(),
                NAME);
    }
}