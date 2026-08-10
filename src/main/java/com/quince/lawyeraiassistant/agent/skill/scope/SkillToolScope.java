package com.quince.lawyeraiassistant.agent.skill.scope;

import com.quince.lawyeraiassistant.agent.skill.context.SkillContext;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 根据当前激活的 Skill 计算 Agent 可使用的 Tool Scope。
 *
 * <p>
 * 当前策略：
 * </p>
 *
 * <ul>
 * <li>存在 SkillContext：只允许 Skill.allowedTools 中的 Tool</li>
 * <li>不存在 SkillContext：不暴露任何 Skill-specific Tool</li>
 * </ul>
 *
 * <p>
 * 当前项目中的 AgentTool 均视为 Skill-specific Tool。
 * 后续如引入真正的 Global Tool，
 * 再扩展为 Global Tool + Skill Tool 双层 Scope。
 * </p>
 */
@Component
public final class SkillToolScope {

    /**
     * 判断指定 Tool 在当前 Skill Scope 中是否允许。
     */
    public boolean isAllowed(
            Optional<SkillContext> skillContext,
            String toolName) {

        Objects.requireNonNull(
                skillContext,
                "SkillContext Optional must not be null");

        if (toolName == null
                || toolName.isBlank()) {

            return false;
        }

        String normalizedToolName = toolName.trim();

        if (skillContext.isEmpty()) {
            return false;
        }

        return skillContext
                .orElseThrow()
                .allowsTool(
                        normalizedToolName);
    }

    /**
     * 从系统提供的 Tool Names 中过滤出
     * 当前 Skill 可以使用的 Tool。
     */
    public List<String> filterAllowed(
            Optional<SkillContext> skillContext,
            Collection<String> toolNames) {

        Objects.requireNonNull(
                skillContext,
                "SkillContext Optional must not be null");

        if (toolNames == null
                || toolNames.isEmpty()) {

            return List.of();
        }

        return toolNames.stream()
                .filter(
                        toolName -> toolName != null
                                && !toolName.isBlank())
                .map(
                        String::trim)
                .distinct()
                .filter(
                        toolName -> isAllowed(
                                skillContext,
                                toolName))
                .toList();
    }
}