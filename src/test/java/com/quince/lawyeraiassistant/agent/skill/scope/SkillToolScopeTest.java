package com.quince.lawyeraiassistant.agent.skill.scope;

import com.quince.lawyeraiassistant.agent.skill.AgentSkill;
import com.quince.lawyeraiassistant.agent.skill.context.SkillContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillToolScopeTest {

    private SkillToolScope skillToolScope;

    @BeforeEach
    void setUp() {

        skillToolScope = new SkillToolScope();
    }

    @Test
    void shouldAllowToolConfiguredBySkill() {

        Optional<SkillContext> skillContext = Optional.of(
                createSkillContext());

        boolean allowed = skillToolScope.isAllowed(
                skillContext,
                "searchLegalKnowledge");

        assertTrue(
                allowed);
    }

    @Test
    void shouldRejectToolNotConfiguredBySkill() {

        Optional<SkillContext> skillContext = Optional.of(
                createSkillContext());

        boolean allowed = skillToolScope.isAllowed(
                skillContext,
                "queryCustomer");

        assertFalse(
                allowed);
    }

    @Test
    void shouldRejectToolWhenNoSkillIsSelected() {

        assertFalse(
                skillToolScope.isAllowed(
                        Optional.empty(),
                        "searchLegalKnowledge"));

        assertFalse(
                skillToolScope.isAllowed(
                        Optional.empty(),
                        "queryCustomer"));
    }

    @Test
    void shouldRejectBlankToolName() {

        assertFalse(
                skillToolScope.isAllowed(
                        Optional.empty(),
                        null));

        assertFalse(
                skillToolScope.isAllowed(
                        Optional.empty(),
                        ""));

        assertFalse(
                skillToolScope.isAllowed(
                        Optional.empty(),
                        " "));
    }

    @Test
    void shouldNormalizeToolName() {

        Optional<SkillContext> skillContext = Optional.of(
                createSkillContext());

        assertTrue(
                skillToolScope.isAllowed(
                        skillContext,
                        "  searchLegalKnowledge  "));
    }

    @Test
    void shouldFilterToolsBySkillScope() {

        Optional<SkillContext> skillContext = Optional.of(
                createSkillContext());

        List<String> result = skillToolScope.filterAllowed(
                skillContext,
                List.of(
                        "searchLegalKnowledge",
                        "queryCustomer",
                        "readDocument"));

        assertEquals(
                List.of(
                        "searchLegalKnowledge"),
                result);
    }

    @Test
    void shouldReturnEmptyToolScopeWhenNoSkillIsSelected() {

        List<String> result = skillToolScope.filterAllowed(
                Optional.empty(),
                List.of(
                        "searchLegalKnowledge",
                        "queryCustomer",
                        "readDocument"));

        assertTrue(
                result.isEmpty());
    }

    @Test
    void shouldRemoveDuplicateTools() {

        List<String> result = skillToolScope.filterAllowed(
                Optional.of(
                        createSkillContext()),
                List.of(
                        "searchLegalKnowledge",
                        "searchLegalKnowledge"));

        assertEquals(
                List.of(
                        "searchLegalKnowledge"),
                result);
    }

    @Test
    void shouldNormalizeToolsWhenFiltering() {

        Optional<SkillContext> skillContext = Optional.of(
                createSkillContext());

        List<String> result = skillToolScope.filterAllowed(
                skillContext,
                List.of(
                        "  searchLegalKnowledge  ",
                        "queryCustomer"));

        assertEquals(
                List.of(
                        "searchLegalKnowledge"),
                result);
    }

    @Test
    void shouldIgnoreBlankToolsWhenFiltering() {

        List<String> result = skillToolScope.filterAllowed(
                Optional.of(
                        createSkillContext()),
                List.of(
                        "searchLegalKnowledge",
                        " ",
                        ""));

        assertEquals(
                List.of(
                        "searchLegalKnowledge"),
                result);
    }

    @Test
    void shouldReturnEmptyWhenToolCollectionIsEmpty() {

        List<String> result = skillToolScope.filterAllowed(
                Optional.of(
                        createSkillContext()),
                List.of());

        assertTrue(
                result.isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenToolCollectionIsNull() {

        List<String> result = skillToolScope.filterAllowed(
                Optional.of(
                        createSkillContext()),
                null);

        assertTrue(
                result.isEmpty());
    }

    @Test
    void shouldRejectNullSkillContextOptional() {

        assertThrows(
                NullPointerException.class,
                () -> skillToolScope.isAllowed(
                        null,
                        "searchLegalKnowledge"));
    }

    private SkillContext createSkillContext() {

        AgentSkill skill = AgentSkill.of(
                "legal-research",
                "Legal Research",
                "用于研究具体法律问题",
                "执行法律研究",
                List.of(
                        "searchLegalKnowledge"),
                Set.of(
                        "legal",
                        "research"));

        return SkillContext.of(
                skill);
    }

    @Test
    void shouldRejectAllToolsWhenSelectedSkillHasNoTools() {

        AgentSkill skill = AgentSkill.of(
                "legal-summary",
                "Legal Summary",
                "总结已有法律材料",
                "基于已有上下文进行总结",
                List.of(),
                Set.of(
                        "legal"));

        Optional<SkillContext> skillContext = Optional.of(
                SkillContext.of(
                        skill));

        assertFalse(
                skillToolScope.isAllowed(
                        skillContext,
                        "searchLegalKnowledge"));
    }

    @Test
    void shouldReturnEmptyToolScopeWhenSelectedSkillHasNoTools() {

        AgentSkill skill = AgentSkill.of(
                "legal-summary",
                "Legal Summary",
                "总结已有法律材料",
                "基于已有上下文进行总结",
                List.of(),
                Set.of(
                        "legal"));

        List<String> result = skillToolScope.filterAllowed(
                Optional.of(
                        SkillContext.of(
                                skill)),
                List.of(
                        "searchLegalKnowledge",
                        "readDocument"));

        assertTrue(
                result.isEmpty());
    }
}