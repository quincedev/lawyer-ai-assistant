package com.quince.lawyeraiassistant.agent.skill;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentSkillRegistryTest {

    @Test
    void shouldRegisterSkills() {

        AgentSkill legalResearch = createSkill(
                "legal-research",
                "Legal Research");

        AgentSkill legalSummary = createSkill(
                "legal-summary",
                "Legal Summary");

        AgentSkillRegistry registry = new AgentSkillRegistry(
                List.of(
                        legalResearch,
                        legalSummary));

        assertEquals(
                2,
                registry.size());

        assertFalse(
                registry.isEmpty());
    }

    @Test
    void shouldFindSkillById() {

        AgentSkill legalResearch = createSkill(
                "legal-research",
                "Legal Research");

        AgentSkillRegistry registry = new AgentSkillRegistry(
                List.of(
                        legalResearch));

        AgentSkill result = registry.findById(
                "legal-research")
                .orElseThrow();

        assertSame(
                legalResearch,
                result);
    }

    @Test
    void shouldNormalizeSkillIdWhenFinding() {

        AgentSkill legalResearch = createSkill(
                "legal-research",
                "Legal Research");

        AgentSkillRegistry registry = new AgentSkillRegistry(
                List.of(
                        legalResearch));

        assertTrue(
                registry.findById(
                        "  legal-research  ")
                        .isPresent());
    }

    @Test
    void shouldReturnEmptyWhenSkillDoesNotExist() {

        AgentSkillRegistry registry = new AgentSkillRegistry(
                List.of(
                        createSkill(
                                "legal-research",
                                "Legal Research")));

        assertTrue(
                registry.findById(
                        "contract-review")
                        .isEmpty());
    }

    @Test
    void shouldRequireExistingSkill() {

        AgentSkill legalResearch = createSkill(
                "legal-research",
                "Legal Research");

        AgentSkillRegistry registry = new AgentSkillRegistry(
                List.of(
                        legalResearch));

        assertSame(
                legalResearch,
                registry.requireById(
                        "legal-research"));
    }

    @Test
    void shouldThrowWhenRequiredSkillDoesNotExist() {

        AgentSkillRegistry registry = new AgentSkillRegistry(
                List.of());

        assertThrows(
                IllegalArgumentException.class,
                () -> registry.requireById(
                        "unknown"));
    }

    @Test
    void shouldCheckWhetherSkillExists() {

        AgentSkillRegistry registry = new AgentSkillRegistry(
                List.of(
                        createSkill(
                                "legal-research",
                                "Legal Research")));

        assertTrue(
                registry.contains(
                        "legal-research"));

        assertFalse(
                registry.contains(
                        "contract-review"));
    }

    @Test
    void shouldListAllSkills() {

        AgentSkill first = createSkill(
                "legal-research",
                "Legal Research");

        AgentSkill second = createSkill(
                "legal-summary",
                "Legal Summary");

        AgentSkillRegistry registry = new AgentSkillRegistry(
                List.of(
                        first,
                        second));

        List<AgentSkill> skills = registry.list();

        assertEquals(
                2,
                skills.size());

        assertTrue(
                skills.contains(
                        first));

        assertTrue(
                skills.contains(
                        second));
    }

    @Test
    void shouldSupportEmptyRegistry() {

        AgentSkillRegistry registry = new AgentSkillRegistry(
                List.of());

        assertTrue(
                registry.isEmpty());

        assertEquals(
                0,
                registry.size());

        assertTrue(
                registry.list()
                        .isEmpty());
    }

    @Test
    void shouldRejectDuplicateSkillIds() {

        AgentSkill first = createSkill(
                "legal-research",
                "Legal Research A");

        AgentSkill second = createSkill(
                "legal-research",
                "Legal Research B");

        assertThrows(
                IllegalArgumentException.class,
                () -> new AgentSkillRegistry(
                        List.of(
                                first,
                                second)));
    }

    @Test
    void shouldReturnEmptyForBlankSkillId() {

        AgentSkillRegistry registry = new AgentSkillRegistry(
                List.of(
                        createSkill(
                                "legal-research",
                                "Legal Research")));

        assertTrue(
                registry.findById(
                        null)
                        .isEmpty());

        assertTrue(
                registry.findById(
                        " ")
                        .isEmpty());
    }

    private AgentSkill createSkill(
            String id,
            String name) {

        return AgentSkill.of(
                id,
                name,
                "用于测试的 Skill",
                "执行测试 Skill",
                List.of(
                        "searchLegalKnowledge"),
                Set.of(
                        "legal"));
    }
}