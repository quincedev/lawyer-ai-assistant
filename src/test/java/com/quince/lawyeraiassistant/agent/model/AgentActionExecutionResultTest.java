package com.quince.lawyeraiassistant.agent.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class AgentActionExecutionResultTest {

    @Test
    void shouldCreateToolResult() {

    ToolObservation observation = mock(
            ToolObservation.class);

    AgentActionExecutionResult result = AgentActionExecutionResult.tool(
            observation);

    assertTrue(
            result.isTool());

    assertFalse(
            result.isReason());

    assertFalse(
            result.isFinalAnswer());

    assertSame(
            observation,
            result.getObservation());

    assertNull(
            result.getContent());
    }
}
