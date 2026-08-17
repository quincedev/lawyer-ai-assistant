package com.quince.lawyeraiassistant.security.authorization.tool.policy;

import java.util.Objects;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.ToolAction;
import com.quince.lawyeraiassistant.agent.skill.scope.SkillToolScope;
import com.quince.lawyeraiassistant.security.authorization.tool.ToolAuthorizationPolicy;
import com.quince.lawyeraiassistant.security.authorization.tool.ToolAuthorizationResult;

/**
 * 基于当前激活 Skill 的 Tool Scope
 * 对 ToolAction 执行授权判断。
 *
 * <p>
 * 当前策略：
 * </p>
 *
 * <ul>
 * <li>存在 SkillContext 且 Tool 在 allowedTools 中：ALLOW</li>
 * <li>不存在 SkillContext：DENY</li>
 * <li>Tool 不属于当前 Skill：DENY</li>
 * </ul>
 *
 * <p>
 * 本 Policy 不负责：
 * </p>
 *
 * <ul>
 * <li>判断 Tool 是否真实存在</li>
 * <li>判断 Tool Risk Level</li>
 * <li>User / Tenant / Role 授权</li>
 * </ul>
 */
@Component
@Order(20)
public final class SkillToolAuthorizationPolicy
        implements ToolAuthorizationPolicy {

    private static final String NAME = "skillToolAuthorization";

    private final SkillToolScope skillToolScope;

    public SkillToolAuthorizationPolicy(
            SkillToolScope skillToolScope) {

        this.skillToolScope = Objects.requireNonNull(
                skillToolScope,
                "skillToolScope must not be null");
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

        boolean allowed = skillToolScope.isAllowed(
                context.getSkillContext(),
                toolName);

        if (allowed) {

            return ToolAuthorizationResult.allow(
                    toolName,
                    NAME);
        }

        if (context.getSkillContext().isEmpty()) {

            return ToolAuthorizationResult.deny(
                    toolName,
                    NAME,
                    "Tool is not allowed because no Skill is active");
        }

        return ToolAuthorizationResult.deny(
                toolName,
                NAME,
                "Tool is not allowed by current Skill");
    }
}