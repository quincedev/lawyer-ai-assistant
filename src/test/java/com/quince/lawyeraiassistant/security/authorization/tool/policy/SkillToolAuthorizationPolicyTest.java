package com.quince.lawyeraiassistant.security.authorization.tool.policy;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.ToolAction;
import com.quince.lawyeraiassistant.agent.skill.AgentSkill;
import com.quince.lawyeraiassistant.agent.skill.context.SkillContext;
import com.quince.lawyeraiassistant.agent.skill.scope.SkillToolScope;
import com.quince.lawyeraiassistant.security.authorization.tool.ToolAuthorizationResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import com.quince.lawyeraiassistant.security.SecurityTest;

@SecurityTest
class SkillToolAuthorizationPolicyTest {

    private SkillToolAuthorizationPolicy policy;

    @BeforeEach
    void setUp() {

        policy = new SkillToolAuthorizationPolicy(
                new SkillToolScope());
    }

    @Test
    void shouldAllowToolAllowedByCurrentSkill() {

        AgentSkill skill = AgentSkill.of(
                "legal-research",
                "Legal Research",
                "用于研究法律问题",
                "执行法律研究",
                List.of(
                        "searchLegalKnowledge"),
                Set.of(
                        "legal",
                        "research"));

        AgentContext context = AgentContext.from(
                "研究劳动合同")
                .withSkillContext(
                        SkillContext.of(
                                skill));

        ToolAuthorizationResult result = policy.authorize(
                context,
                ToolAction.of(
                        "task-1",
                        "searchLegalKnowledge"));

        assertTrue(result.isAllowed());
    }

    @Test
    void shouldDenyToolNotAllowedByCurrentSkill() {

        AgentSkill skill = AgentSkill.of(
                "legal-summary",
                "Legal Summary",
                "用于总结法律材料",
                "总结已有材料",
                List.of(),
                Set.of(
                        "legal",
                        "summary"));

        AgentContext context = AgentContext.from(
                "总结材料")
                .withSkillContext(
                        SkillContext.of(
                                skill));

        ToolAuthorizationResult result = policy.authorize(
                context,
                ToolAction.of(
                        "task-1",
                        "searchLegalKnowledge"));

        assertTrue(result.isDenied());

        assertEquals(
                "Tool is not allowed by current Skill",
                result.reason());
    }

    @Test
    void shouldDenyToolWhenNoSkillIsActive() {

        AgentContext context = AgentContext.from(
                "普通问题");

        ToolAuthorizationResult result = policy.authorize(
                context,
                ToolAction.of(
                        "task-1",
                        "searchLegalKnowledge"));

        assertTrue(result.isDenied());

        assertEquals(
                "Tool is not allowed because no Skill is active",
                result.reason());
    }

    @Test
    void shouldRejectNullScope() {

        assertThrows(
                NullPointerException.class,
                () -> new SkillToolAuthorizationPolicy(
                        null));
    }
}