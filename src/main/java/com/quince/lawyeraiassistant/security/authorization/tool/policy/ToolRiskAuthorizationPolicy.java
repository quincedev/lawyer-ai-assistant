package com.quince.lawyeraiassistant.security.authorization.tool.policy;

import java.util.Objects;
import java.util.Optional;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.ToolAction;
import com.quince.lawyeraiassistant.security.authorization.tool.ToolAuthorizationPolicy;
import com.quince.lawyeraiassistant.security.authorization.tool.ToolAuthorizationResult;
import com.quince.lawyeraiassistant.security.authorization.tool.risk.ToolRiskLevel;
import com.quince.lawyeraiassistant.security.authorization.tool.risk.ToolRiskProfile;
import com.quince.lawyeraiassistant.security.authorization.tool.risk.ToolRiskRegistry;

@Component
@Order(30)
public final class ToolRiskAuthorizationPolicy
        implements ToolAuthorizationPolicy {

    private static final String NAME = "toolRiskAuthorization";

    private final ToolRiskRegistry riskRegistry;

    public ToolRiskAuthorizationPolicy(
            ToolRiskRegistry riskRegistry) {

        this.riskRegistry = Objects.requireNonNull(
                riskRegistry,
                "riskRegistry must not be null");
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

        Optional<ToolRiskProfile> optionalProfile = riskRegistry.find(
                toolName);

        if (optionalProfile.isEmpty()) {

            return ToolAuthorizationResult.deny(
                    toolName,
                    NAME,
                    "Tool risk profile is not configured");
        }

        ToolRiskProfile profile = optionalProfile.get();

        if (profile.riskLevel() == ToolRiskLevel.HIGH) {

            return ToolAuthorizationResult.deny(
                    toolName,
                    NAME,
                    "High-risk Tool requires explicit approval");
        }

        return ToolAuthorizationResult.allow(
                toolName,
                NAME);
    }
}