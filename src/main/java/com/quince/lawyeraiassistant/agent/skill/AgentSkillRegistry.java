package com.quince.lawyeraiassistant.agent.skill;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class AgentSkillRegistry {

    private final Map<String, AgentSkill> skills;

    public AgentSkillRegistry(
            Collection<AgentSkill> skills) {

        if (skills == null
                || skills.isEmpty()) {

            this.skills = Map.of();

            return;
        }

        Map<String, AgentSkill> registry = new LinkedHashMap<>();

        for (AgentSkill skill : skills) {

            Objects.requireNonNull(
                    skill,
                    "Agent skill must not be null");

            AgentSkill existing = registry.putIfAbsent(
                    skill.getId(),
                    skill);

            if (existing != null) {

                throw new IllegalArgumentException(
                        "Duplicate skill id: "
                                + skill.getId());
            }
        }

        this.skills = Map.copyOf(
                registry);
    }

    /**
     * 根据 Skill ID 查询。
     */
    public Optional<AgentSkill> findById(
            String skillId) {

        if (skillId == null
                || skillId.isBlank()) {

            return Optional.empty();
        }

        return Optional.ofNullable(
                skills.get(
                        skillId.trim()));
    }

    /**
     * 根据 Skill ID 获取。
     *
     * Skill 不存在时直接失败。
     */
    public AgentSkill requireById(
            String skillId) {

        return findById(
                skillId)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Unknown skill: "
                                        + skillId));
    }

    /**
     * 判断 Skill 是否存在。
     */
    public boolean contains(
            String skillId) {

        return findById(
                skillId)
                .isPresent();
    }

    /**
     * 返回所有 Skill。
     */
    public List<AgentSkill> list() {

        return List.copyOf(
                skills.values());
    }

    public int size() {

        return skills.size();
    }

    public boolean isEmpty() {

        return skills.isEmpty();
    }
}