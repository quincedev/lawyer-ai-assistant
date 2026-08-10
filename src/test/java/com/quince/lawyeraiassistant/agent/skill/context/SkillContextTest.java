package com.quince.lawyeraiassistant.agent.skill.context;

import com.quince.lawyeraiassistant.agent.skill.AgentSkill;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillContextTest {

    @Test
    void shouldCreateSkillContext() {

        AgentSkill skill = createSkill();

        SkillContext context = SkillContext.of(
                skill);

        assertEquals(
                skill,
                context.getSkill());

        assertEquals(
                "legal-research",
                context.getSkillId());

        assertEquals(
                "Legal Research",
                context.getSkillName());

        assertEquals(
                "执行法律研究",
                context.getInstructions());
    }

    @Test
    void shouldExposeAllowedTools() {

        SkillContext context = SkillContext.of(
                createSkill());

        assertEquals(
                List.of(
                        "searchLegalKnowledge"),
                context.getAllowedTools());

        assertTrue(
                context.allowsTool(
                        "searchLegalKnowledge"));

        assertFalse(
                context.allowsTool(
                        "queryCustomer"));
    }

    @Test
    void shouldRejectNullSkill() {

        assertThrows(
                NullPointerException.class,
                () -> SkillContext.of(
                        null));
    }

    private AgentSkill createSkill() {

        return AgentSkill.of(
                "legal-research",
                "Legal Research",
                "用于研究具体法律问题",
                "执行法律研究",
                List.of(
                        "searchLegalKnowledge"),
                Set.of(
                        "legal",
                        "research"));
    }
}