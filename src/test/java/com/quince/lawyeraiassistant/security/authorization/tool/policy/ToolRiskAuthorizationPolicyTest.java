package com.quince.lawyeraiassistant.security.authorization.tool.policy;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.ToolAction;
import com.quince.lawyeraiassistant.security.authorization.tool.ToolAuthorizationResult;
import com.quince.lawyeraiassistant.security.authorization.tool.risk.ToolRiskLevel;
import com.quince.lawyeraiassistant.security.authorization.tool.risk.ToolRiskProfile;
import com.quince.lawyeraiassistant.security.authorization.tool.risk.ToolRiskRegistry;
import com.quince.lawyeraiassistant.security.authorization.tool.risk.ToolSideEffectType;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import com.quince.lawyeraiassistant.security.SecurityTest;

@SecurityTest
class ToolRiskAuthorizationPolicyTest {

    @Test
    void shouldAllowLowRiskTool() {

        ToolRiskRegistry registry = new ToolRiskRegistry(
                List.of(
                        ToolRiskProfile.lowReadOnly(
                                "searchLegalKnowledge")));

        ToolRiskAuthorizationPolicy policy = new ToolRiskAuthorizationPolicy(
                registry);

        ToolAuthorizationResult result = policy.authorize(
                AgentContext.from("研究法律问题"),
                ToolAction.of(
                        "task-1",
                        "searchLegalKnowledge"));

        assertTrue(result.isAllowed());
    }

    @Test
    void shouldAllowMediumRiskTool() {

        ToolRiskRegistry registry = new ToolRiskRegistry(
                List.of(
                        new ToolRiskProfile(
                                "createDraft",
                                ToolRiskLevel.MEDIUM,
                                ToolSideEffectType.WRITE)));

        ToolRiskAuthorizationPolicy policy = new ToolRiskAuthorizationPolicy(
                registry);

        ToolAuthorizationResult result = policy.authorize(
                AgentContext.from("创建草稿"),
                ToolAction.of(
                        "task-1",
                        "createDraft"));

        assertTrue(result.isAllowed());
    }

    @Test
    void shouldDenyHighRiskTool() {

        ToolRiskRegistry registry = new ToolRiskRegistry(
                List.of(
                        new ToolRiskProfile(
                                "deleteCase",
                                ToolRiskLevel.HIGH,
                                ToolSideEffectType.DESTRUCTIVE)));

        ToolRiskAuthorizationPolicy policy = new ToolRiskAuthorizationPolicy(
                registry);

        ToolAuthorizationResult result = policy.authorize(
                AgentContext.from("删除案件"),
                ToolAction.of(
                        "task-1",
                        "deleteCase"));

        assertTrue(result.isDenied());

        assertEquals(
                "High-risk Tool requires explicit approval",
                result.reason());
    }

    @Test
    void shouldDenyToolWithoutRiskProfile() {

        ToolRiskAuthorizationPolicy policy = new ToolRiskAuthorizationPolicy(
                new ToolRiskRegistry(
                        List.of()));

        ToolAuthorizationResult result = policy.authorize(
                AgentContext.from("测试"),
                ToolAction.of(
                        "task-1",
                        "unknownRiskTool"));

        assertTrue(result.isDenied());

        assertEquals(
                "Tool risk profile is not configured",
                result.reason());
    }
}