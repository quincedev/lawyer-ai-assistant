package com.quince.lawyeraiassistant.agent.skill;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Agent 可复用专业能力的声明式定义。
 *
 * <p>
 * AgentSkill 本身不负责执行任务。
 * 它描述：
 * </p>
 *
 * <ul>
 * <li>这个能力是什么</li>
 * <li>适合解决什么问题</li>
 * <li>执行时应该遵循什么指令</li>
 * <li>允许使用哪些 Tool</li>
 * </ul>
 */
@Getter
@ToString
@EqualsAndHashCode
public final class AgentSkill {

    /**
     * Skill 的稳定系统标识。
     *
     * 例如：
     * legal-research
     */
    private final String id;

    /**
     * Skill 的展示名称。
     *
     * 例如：
     * Legal Research
     */
    private final String name;

    /**
     * 描述 Skill 适合解决什么类型的问题。
     *
     * 后续 Skill Selector 会使用这个字段。
     */
    private final String description;

    /**
     * Skill 的专业执行指令。
     *
     * 后续会注入 Planning / Action Selection /
     * Reflection / Final Answer 等 Prompt Context。
     */
    private final String instructions;

    /**
     * Skill 激活后允许 Agent 使用的 Tool Name。
     */
    private final List<String> allowedTools;

    /**
     * Skill 分类标签。
     *
     * 第一版主要用于描述和未来检索扩展。
     */
    private final Set<String> tags;

    private AgentSkill(
            String id,
            String name,
            String description,
            String instructions,
            List<String> allowedTools,
            Set<String> tags) {

        this.id = requireText(
                id,
                "Skill id must not be blank");

        this.name = requireText(
                name,
                "Skill name must not be blank");

        this.description = requireText(
                description,
                "Skill description must not be blank");

        this.instructions = requireText(
                instructions,
                "Skill instructions must not be blank");

        this.allowedTools = normalizeAllowedTools(
                allowedTools);

        this.tags = normalizeTags(
                tags);
    }

    public static AgentSkill of(
            String id,
            String name,
            String description,
            String instructions,
            List<String> allowedTools,
            Set<String> tags) {

        return new AgentSkill(
                id,
                name,
                description,
                instructions,
                allowedTools,
                tags);
    }

    /**
     * 判断 Skill 是否允许指定 Tool。
     */
    public boolean allowsTool(
            String toolName) {

        if (toolName == null
                || toolName.isBlank()) {

            return false;
        }

        String normalized = toolName.trim();

        return allowedTools.contains(
                normalized);
    }

    /**
     * Skill 是否不依赖任何 Tool。
     *
     * 例如纯 Reasoning / Summary Skill。
     */
    public boolean hasNoTools() {

        return allowedTools.isEmpty();
    }

    public int allowedToolCount() {

        return allowedTools.size();
    }

    private static String requireText(
            String value,
            String message) {

        Objects.requireNonNull(
                value,
                message);

        String normalized = value.trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    message);
        }

        return normalized;
    }

    private static List<String> normalizeAllowedTools(
            List<String> allowedTools) {

        if (allowedTools == null
                || allowedTools.isEmpty()) {

            return List.of();
        }

        return allowedTools.stream()
                .map(
                        toolName -> requireText(
                                toolName,
                                "Allowed tool name must not be blank"))
                .distinct()
                .toList();
    }

    private static Set<String> normalizeTags(
            Set<String> tags) {

        if (tags == null
                || tags.isEmpty()) {

            return Set.of();
        }

        return tags.stream()
                .map(
                        tag -> requireText(
                                tag,
                                "Skill tag must not be blank"))
                .collect(
                        java.util.stream.Collectors.toUnmodifiableSet());
    }
}