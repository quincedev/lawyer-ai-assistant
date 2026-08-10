package com.quince.lawyeraiassistant.agent.skill.context;

import com.quince.lawyeraiassistant.agent.skill.AgentSkill;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 当前一次 Agent Run 中激活的 Skill Runtime Context。
 *
 * <p>
 * AgentSkill 描述系统拥有的能力；
 * SkillContext 描述当前 Agent Run 正在使用的能力。
 * </p>
 */
public final class SkillContext {

    private final AgentSkill skill;

    private SkillContext(
            AgentSkill skill) {

        this.skill = Objects.requireNonNull(
                skill,
                "AgentSkill must not be null");
    }

    public static SkillContext of(
            AgentSkill skill) {

        return new SkillContext(
                skill);
    }

    public AgentSkill getSkill() {

        return skill;
    }

    public String getSkillId() {

        return skill.getId();
    }

    public String getSkillName() {

        return skill.getName();
    }

    public String getDescription() {

        return skill.getDescription();
    }

    public String getInstructions() {

        return skill.getInstructions();
    }

    public List<String> getAllowedTools() {

        return skill.getAllowedTools();
    }

    public boolean allowsTool(
            String toolName) {

        return skill.allowsTool(
                toolName);
    }

    public boolean hasNoTools() {

        return skill.hasNoTools();
    }

    public Optional<String> instructions() {

        if (skill.getInstructions() == null
                || skill.getInstructions().isBlank()) {

            return Optional.empty();
        }

        return Optional.of(
                skill.getInstructions());
    }
}