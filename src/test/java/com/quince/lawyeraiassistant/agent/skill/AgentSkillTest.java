package com.quince.lawyeraiassistant.agent.skill;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentSkillTest {

    @Test
    void shouldCreateAgentSkill() {

        AgentSkill skill = AgentSkill.of(
                "legal-research",
                "Legal Research",
                "用于研究法律问题",
                "检索法律依据并形成法律分析",
                List.of(
                        "searchLegalKnowledge"),
                Set.of(
                        "legal",
                        "research"));

        assertEquals(
                "legal-research",
                skill.getId());

        assertEquals(
                "Legal Research",
                skill.getName());

        assertEquals(
                "用于研究法律问题",
                skill.getDescription());

        assertEquals(
                "检索法律依据并形成法律分析",
                skill.getInstructions());

        assertEquals(
                List.of(
                        "searchLegalKnowledge"),
                skill.getAllowedTools());

        assertEquals(
                2,
                skill.getTags()
                        .size());
    }

    @Test
    void shouldNormalizeTextFields() {

        AgentSkill skill = AgentSkill.of(
                "  legal-research  ",
                "  Legal Research  ",
                "  用于研究法律问题  ",
                "  执行法律研究  ",
                List.of(
                        "  searchLegalKnowledge  "),
                Set.of(
                        "  legal  "));

        assertEquals(
                "legal-research",
                skill.getId());

        assertEquals(
                "Legal Research",
                skill.getName());

        assertEquals(
                "searchLegalKnowledge",
                skill.getAllowedTools()
                        .getFirst());

        assertTrue(
                skill.getTags()
                        .contains(
                                "legal"));
    }

    @Test
    void shouldRemoveDuplicateTools() {

        AgentSkill skill = AgentSkill.of(
                "legal-research",
                "Legal Research",
                "用于研究法律问题",
                "执行法律研究",
                List.of(
                        "searchLegalKnowledge",
                        "searchLegalKnowledge"),
                Set.of());

        assertEquals(
                1,
                skill.allowedToolCount());
    }

    @Test
    void shouldAllowConfiguredTool() {

        AgentSkill skill = createSkill();

        assertTrue(
                skill.allowsTool(
                        "searchLegalKnowledge"));

        assertFalse(
                skill.allowsTool(
                        "queryCustomer"));
    }

    @Test
    void shouldReturnFalseForBlankToolName() {

        AgentSkill skill = createSkill();

        assertFalse(
                skill.allowsTool(
                        null));

        assertFalse(
                skill.allowsTool(
                        " "));
    }

    @Test
    void shouldSupportSkillWithoutTools() {

        AgentSkill skill = AgentSkill.of(
                "legal-summary",
                "Legal Summary",
                "总结已有法律材料",
                "基于已有信息形成摘要",
                List.of(),
                Set.of(
                        "legal"));

        assertTrue(
                skill.hasNoTools());

        assertEquals(
                0,
                skill.allowedToolCount());
    }

    @Test
    void shouldRejectBlankId() {

        assertThrows(
                IllegalArgumentException.class,
                () -> AgentSkill.of(
                        " ",
                        "Legal Research",
                        "用于研究法律问题",
                        "执行法律研究",
                        List.of(),
                        Set.of()));
    }

    @Test
    void shouldRejectBlankAllowedTool() {

        assertThrows(
                IllegalArgumentException.class,
                () -> AgentSkill.of(
                        "legal-research",
                        "Legal Research",
                        "用于研究法律问题",
                        "执行法律研究",
                        List.of(
                                " "),
                        Set.of()));
    }

    @Test
    void shouldRejectBlankTag() {

        assertThrows(
                IllegalArgumentException.class,
                () -> AgentSkill.of(
                        "legal-research",
                        "Legal Research",
                        "用于研究法律问题",
                        "执行法律研究",
                        List.of(),
                        Set.of(
                                " ")));
    }

    private AgentSkill createSkill() {

        return AgentSkill.of(
                "legal-research",
                "Legal Research",
                "用于研究法律问题",
                "执行法律研究",
                List.of(
                        "searchLegalKnowledge"),
                Set.of(
                        "legal",
                        "research"));
    }
}