package com.quince.lawyeraiassistant.security.authorization.tool.policy;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.ToolAction;
import com.quince.lawyeraiassistant.security.authorization.tool.ToolAuthorizationResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.quince.lawyeraiassistant.security.SecurityTest;

@SecurityTest
class SecurityContextToolAuthorizationPolicyTest {

    private SecurityContextToolAuthorizationPolicy policy;

    @BeforeEach
    void setUp() {

        policy = new SecurityContextToolAuthorizationPolicy();
    }

    @Test
    void shouldAllowAuthorizationEvaluationWhenSecurityContextExists() {

        AgentContext context = AgentContext.from(
                "研究劳动合同");

        ToolAction action = ToolAction.of(
                "task-1",
                "searchLegalKnowledge",
                Map.of(
                        "legalQuestion",
                        "劳动合同解除"));

        ToolAuthorizationResult result = policy.authorize(
                context,
                action);

        assertTrue(
                result.isAllowed());

        assertEquals(
                "securityContextToolAuthorization",
                result.policyName());
    }

    @Test
    void shouldDenyWhenSecurityContextIsMissing() {

        AgentContext context = AgentContext.builder()
                .goal(
                        "研究劳动合同")
                .build();

        ToolAction action = ToolAction.of(
                "task-1",
                "searchLegalKnowledge",
                Map.of(
                        "legalQuestion",
                        "劳动合同解除"));

        ToolAuthorizationResult result = policy.authorize(
                context,
                action);

        assertTrue(
                result.isDenied());

        assertEquals(
                "Tool authorization denied because LegalSecurityContext is missing",
                result.reason());
    }
}