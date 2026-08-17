package com.quince.lawyeraiassistant.security.authorization.tool;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.ToolAction;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.quince.lawyeraiassistant.security.SecurityTest;

@SecurityTest
class DefaultToolAuthorizationServiceTest {

    @Test
    void shouldAllowWhenAllPoliciesAllow() {

        ToolAuthorizationPolicy first = mock(ToolAuthorizationPolicy.class);

        ToolAuthorizationPolicy second = mock(ToolAuthorizationPolicy.class);

        AgentContext context = AgentContext.from(
                "研究法律问题");

        ToolAction action = ToolAction.of(
                "task-1",
                "searchLegalKnowledge");

        when(
                first.authorize(
                        context,
                        action))
                .thenReturn(
                        ToolAuthorizationResult.allow(
                                "searchLegalKnowledge",
                                "first"));

        when(
                second.authorize(
                        context,
                        action))
                .thenReturn(
                        ToolAuthorizationResult.allow(
                                "searchLegalKnowledge",
                                "second"));

        DefaultToolAuthorizationService service = new DefaultToolAuthorizationService(
                List.of(
                        first,
                        second));

        ToolAuthorizationResult result = service.authorize(
                context,
                action);

        assertTrue(result.isAllowed());

        assertEquals(
                "toolAuthorization",
                result.policyName());

        verify(first).authorize(
                context,
                action);

        verify(second).authorize(
                context,
                action);
    }

    @Test
    void shouldReturnFirstDenyAndStopRemainingPolicies() {

        ToolAuthorizationPolicy first = mock(ToolAuthorizationPolicy.class);

        ToolAuthorizationPolicy second = mock(ToolAuthorizationPolicy.class);

        ToolAuthorizationPolicy third = mock(ToolAuthorizationPolicy.class);

        AgentContext context = AgentContext.from(
                "测试");

        ToolAction action = ToolAction.of(
                "task-1",
                "deleteCase");

        when(
                first.authorize(
                        context,
                        action))
                .thenReturn(
                        ToolAuthorizationResult.allow(
                                "deleteCase",
                                "first"));

        ToolAuthorizationResult denied = ToolAuthorizationResult.deny(
                "deleteCase",
                "second",
                "Denied");

        when(
                second.authorize(
                        context,
                        action))
                .thenReturn(
                        denied);

        DefaultToolAuthorizationService service = new DefaultToolAuthorizationService(
                List.of(
                        first,
                        second,
                        third));

        ToolAuthorizationResult result = service.authorize(
                context,
                action);

        assertSame(
                denied,
                result);

        verify(first).authorize(
                context,
                action);

        verify(second).authorize(
                context,
                action);

        verify(
                third,
                never())
                .authorize(
                        context,
                        action);
    }

    @Test
    void shouldRejectEmptyPolicyList() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new DefaultToolAuthorizationService(
                        List.of()));

        assertEquals(
                "At least one ToolAuthorizationPolicy is required",
                exception.getMessage());
    }

    @Test
    void shouldRejectNullPolicyResult() {

        ToolAuthorizationPolicy policy = mock(
                ToolAuthorizationPolicy.class);

        when(
                policy.name())
                .thenReturn(
                        "brokenPolicy");

        AgentContext context = AgentContext.from(
                "测试");

        ToolAction action = ToolAction.of(
                "task-1",
                "searchLegalKnowledge");

        when(
                policy.authorize(
                        context,
                        action))
                .thenReturn(
                        null);

        DefaultToolAuthorizationService service = new DefaultToolAuthorizationService(
                List.of(
                        policy));

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> service.authorize(
                        context,
                        action));

        assertEquals(
                "ToolAuthorizationPolicy must not return null: brokenPolicy",
                exception.getMessage());
    }
}