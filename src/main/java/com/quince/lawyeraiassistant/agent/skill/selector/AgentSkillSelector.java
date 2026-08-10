package com.quince.lawyeraiassistant.agent.skill.selector;

import com.quince.lawyeraiassistant.agent.skill.AgentSkill;

import java.util.Optional;

public interface AgentSkillSelector {

    Optional<AgentSkill> select(String goal);
}