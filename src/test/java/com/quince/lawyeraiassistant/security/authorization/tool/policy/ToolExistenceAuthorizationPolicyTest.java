package com.quince.lawyeraiassistant.security.authorization.tool.policy;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.ToolAction;
import com.quince.lawyeraiassistant.agent.tool.AgentToolRegistry;
import com.quince.lawyeraiassistant.security.authorization.tool.ToolAuthorizationResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.quince.lawyeraiassistant.security.SecurityTest;

@SecurityTest
class ToolExistenceAuthorizationPolicyTest {

    private AgentToolRegistry toolRegistry;

    private ToolExistenceAuthorizationPolicy policy;

    @BeforeEach
    void setUp() {

        toolRegistry = mock(AgentToolRegistry.class);

        policy = new ToolExistenceAuthorizationPolicy(
                toolRegistry);
    }

    @Test
    void shouldAllowExistingTool() {

        ToolAction action = ToolAction.of(
                "task-1",
                "searchLegalKnowledge");

        when(
                toolRegistry.contains(
                        "searchLegalKnowledge"))
                .thenReturn(true);

        ToolAuthorizationResult result = policy.authorize(
                AgentContext.from("研究劳动合同"),
                action);

        assertTrue(result.isAllowed());

        assertEquals(
                "searchLegalKnowledge",
                result.toolName());

        assertEquals(
                "toolExistenceAuthorization",
                result.policyName());
    }

    @Test
    void shouldDenyUnknownTool() {

        ToolAction action = ToolAction.of(
                "task-1",
                "deleteEntireDatabase");

        when(
                toolRegistry.contains(
                        "deleteEntireDatabase"))
                .thenReturn(false);

        ToolAuthorizationResult result = policy.authorize(
                AgentContext.from("测试"),
                action);

        assertTrue(result.isDenied());

        assertEquals(
                "Tool does not exist",
                result.reason());
    }

    @Test
    void shouldRejectNullContext() {

        assertThrows(
                NullPointerException.class,
                () -> policy.authorize(
                        null,
                        ToolAction.of(
                                "task-1",
                                "searchLegalKnowledge")));
    }

    @Test
    void shouldRejectNullAction() {

        assertThrows(
                NullPointerException.class,
                () -> policy.authorize(
                        AgentContext.from("测试"),
                        null));
    }

    @Test
    void shouldRejectNullRegistry() {

        assertThrows(
                NullPointerException.class,
                () -> new ToolExistenceAuthorizationPolicy(
                        null));
    }
}