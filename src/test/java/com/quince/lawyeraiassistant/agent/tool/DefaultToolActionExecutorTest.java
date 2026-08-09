package com.quince.lawyeraiassistant.agent.tool;

import com.quince.lawyeraiassistant.agent.model.ToolAction;
import com.quince.lawyeraiassistant.agent.model.ToolExecutionResult;
import com.quince.lawyeraiassistant.agent.model.ToolObservation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultToolActionExecutorTest {

    private AgentTool agentTool;

    private AgentToolRegistry toolRegistry;

    private DefaultToolActionExecutor executor;

    @BeforeEach
    void setUp() {

        agentTool = mock(
                AgentTool.class);

        when(
                agentTool.name())
                .thenReturn(
                        "searchLegalKnowledge");

        toolRegistry = new AgentToolRegistry(
                List.of(
                        agentTool));

        executor = new DefaultToolActionExecutor(
                toolRegistry);
    }

    @Test
    void shouldExecuteToolSuccessfully() {

        ToolAction action = ToolAction.of(
                "task-1",
                "searchLegalKnowledge",
                Map.of(
                        "legalQuestion",
                        "违法解除劳动合同"));

        when(
                agentTool.execute(
                        action))
                .thenReturn(
                        ToolExecutionResult.success(
                                "劳动合同法第八十七条规定..."));

        ToolObservation observation = executor.execute(
                action);

        assertTrue(
                observation.isSuccess());

        assertEquals(
                "task-1",
                observation.getTaskId());

        assertEquals(
                "searchLegalKnowledge",
                observation.getToolName());

        assertEquals(
                "劳动合同法第八十七条规定...",
                observation.getContent());

        verify(
                agentTool)
                .execute(
                        action);
    }

    @Test
    void shouldConvertFailedResultToFailedObservation() {

        ToolAction action = ToolAction.of(
                "task-1",
                "searchLegalKnowledge");

        when(
                agentTool.execute(
                        action))
                .thenReturn(
                        ToolExecutionResult.failure(
                                "VectorStore unavailable"));

        ToolObservation observation = executor.execute(
                action);

        assertTrue(
                observation.isFailure());

        assertEquals(
                "task-1",
                observation.getTaskId());

        assertEquals(
                "searchLegalKnowledge",
                observation.getToolName());

        assertEquals(
                "VectorStore unavailable",
                observation.getErrorMessage());
    }

    @Test
    void shouldRejectNullAction() {

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> executor.execute(
                        null));

        assertEquals(
                "ToolAction must not be null",
                exception.getMessage());
    }

    @Test
    void shouldRejectNullToolRegistry() {

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new DefaultToolActionExecutor(
                        null));

        assertEquals(
                "toolRegistry must not be null",
                exception.getMessage());
    }

    @Test
    void shouldRejectNullToolExecutionResult() {

        ToolAction action = ToolAction.of(
                "task-1",
                "searchLegalKnowledge");

        when(
                agentTool.execute(
                        action))
                .thenReturn(
                        null);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> executor.execute(
                        action));

        assertEquals(
                "ToolExecutionResult must not be null",
                exception.getMessage());
    }
}