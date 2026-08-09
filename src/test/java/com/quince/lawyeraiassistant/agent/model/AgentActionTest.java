package com.quince.lawyeraiassistant.agent.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentActionTest {

    @Test
    void shouldCreateToolAction() {

        ToolAction toolAction = ToolAction.of(
                "task-1",
                "searchLegalKnowledge",
                Map.of(
                        "legalQuestion",
                        "违法解除劳动合同需要承担什么法律责任"));

        AgentAction action = AgentAction.tool(
                toolAction);

        assertEquals(
                "task-1",
                action.getTaskId());

        assertEquals(
                AgentActionType.TOOL,
                action.getType());

        assertTrue(
                action.isTool());

        assertFalse(
                action.isReason());

        assertFalse(
                action.isFinalAnswer());

        assertSame(
                toolAction,
                action.getToolAction());

        assertSame(
                toolAction,
                action.requireToolAction());
    }

    @Test
    void shouldCreateReasonAction() {

        AgentAction action = AgentAction.reason(
                "task-2");

        assertEquals(
                "task-2",
                action.getTaskId());

        assertEquals(
                AgentActionType.REASON,
                action.getType());

        assertFalse(
                action.isTool());

        assertTrue(
                action.isReason());

        assertFalse(
                action.isFinalAnswer());

        assertNull(
                action.getToolAction());
    }

    @Test
    void shouldCreateFinalAnswerAction() {

        AgentAction action = AgentAction.finalAnswer(
                "task-3");

        assertEquals(
                "task-3",
                action.getTaskId());

        assertEquals(
                AgentActionType.FINAL_ANSWER,
                action.getType());

        assertFalse(
                action.isTool());

        assertFalse(
                action.isReason());

        assertTrue(
                action.isFinalAnswer());

        assertNull(
                action.getToolAction());
    }

    @Test
    void shouldNormalizeReasonTaskId() {

        AgentAction action = AgentAction.reason(
                "  task-1  ");

        assertEquals(
                "task-1",
                action.getTaskId());
    }

    @Test
    void shouldNormalizeFinalAnswerTaskId() {

        AgentAction action = AgentAction.finalAnswer(
                "  task-5  ");

        assertEquals(
                "task-5",
                action.getTaskId());
    }

    @Test
    void shouldRejectNullToolAction() {

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> AgentAction.tool(
                        null));

        assertEquals(
                "ToolAction must not be null",
                exception.getMessage());
    }

    @Test
    void shouldRejectNullTaskId() {

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> AgentAction.reason(
                        null));

        assertEquals(
                "Task id must not be null",
                exception.getMessage());
    }

    @Test
    void shouldRejectBlankTaskId() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AgentAction.finalAnswer(
                        "   "));

        assertEquals(
                "Task id must not be blank",
                exception.getMessage());
    }

    @Test
    void shouldRejectToolAccessFromReasonAction() {

        AgentAction action = AgentAction.reason(
                "task-1");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                action::requireToolAction);

        assertEquals(
                "Current AgentAction is not a TOOL action",
                exception.getMessage());
    }

    @Test
    void shouldRejectToolAccessFromFinalAnswerAction() {

        AgentAction action = AgentAction.finalAnswer(
                "task-1");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                action::requireToolAction);

        assertEquals(
                "Current AgentAction is not a TOOL action",
                exception.getMessage());
    }
}