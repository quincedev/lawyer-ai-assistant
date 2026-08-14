package com.quince.lawyeraiassistant.agent.tool.legal;

import com.quince.lawyeraiassistant.agent.model.ToolAction;
import com.quince.lawyeraiassistant.agent.model.ToolExecutionResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;

import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class McpLegalKnowledgeToolTest {

    private SyncMcpToolCallbackProvider toolCallbackProvider;

    private ToolCallback toolCallback;

    private ToolDefinition toolDefinition;

    private ObjectMapper objectMapper;

    private McpLegalKnowledgeTool tool;

    @BeforeEach
    void setUp() {

        toolCallbackProvider = mock(
                SyncMcpToolCallbackProvider.class);

        toolCallback = mock(
                ToolCallback.class);

        toolDefinition = mock(
                ToolDefinition.class);

        objectMapper = new ObjectMapper();

        when(
                toolDefinition.name())
                .thenReturn(
                        LegalKnowledgeTool.TOOL_NAME);

        when(
                toolCallback.getToolDefinition())
                .thenReturn(
                        toolDefinition);

        when(
                toolCallbackProvider.getToolCallbacks())
                .thenReturn(
                        new ToolCallback[] {
                                toolCallback
                        });

        tool = new McpLegalKnowledgeTool(
                toolCallbackProvider,
                objectMapper);
    }

    @Test
    void shouldReturnExpectedToolName() {

        assertEquals(
                LegalKnowledgeTool.TOOL_NAME,
                tool.name());
    }

    @Test
    void shouldExecuteMcpToolSuccessfully() {

        ToolAction action = ToolAction.of(
                "task-1",
                LegalKnowledgeTool.TOOL_NAME,
                Map.of(
                        LegalKnowledgeTool.LEGAL_QUESTION_ARGUMENT,
                        "违法解除劳动合同的赔偿标准"));

        when(
                toolCallback.call(
                        anyString()))
                .thenReturn(
                        "劳动合同法第八十七条规定，用人单位违法解除劳动合同应支付赔偿金。");

        ToolExecutionResult result = tool.execute(
                action);

        assertTrue(
                result.isSuccess());

        assertFalse(
                result.isFailure());

        assertNull(
                result.getErrorMessage());

        assertEquals(
                "劳动合同法第八十七条规定，用人单位违法解除劳动合同应支付赔偿金。",
                result.getContent());

        ArgumentCaptor<String> inputCaptor = ArgumentCaptor.forClass(
                String.class);

        verify(
                toolCallback)
                .call(
                        inputCaptor.capture());

        String toolInput = inputCaptor.getValue();

        assertTrue(
                toolInput.contains(
                        "\"legalQuestion\""));

        assertTrue(
                toolInput.contains(
                        "违法解除劳动合同的赔偿标准"));
    }

    @Test
    void shouldSerializeAgentToolArgumentsAsJson() {

        ToolAction action = ToolAction.of(
                "task-2",
                LegalKnowledgeTool.TOOL_NAME,
                Map.of(
                        LegalKnowledgeTool.LEGAL_QUESTION_ARGUMENT,
                        "劳动合同法第87条"));

        when(
                toolCallback.call(
                        anyString()))
                .thenReturn(
                        "法律检索结果");

        tool.execute(
                action);

        ArgumentCaptor<String> inputCaptor = ArgumentCaptor.forClass(
                String.class);

        verify(
                toolCallback)
                .call(
                        inputCaptor.capture());

        String json = inputCaptor.getValue();

        assertTrue(
                json.startsWith(
                        "{"));

        assertTrue(
                json.endsWith(
                        "}"));

        assertTrue(
                json.contains(
                        "\"legalQuestion\""));

        assertTrue(
                json.contains(
                        "劳动合同法第87条"));
    }

    @Test
    void shouldConvertMcpRuntimeExceptionToFailedResult() {

        ToolAction action = createAction(
                "劳动合同解除条件");

        when(
                toolCallback.call(
                        anyString()))
                .thenThrow(
                        new IllegalStateException(
                                "MCP server unavailable"));

        ToolExecutionResult result = tool.execute(
                action);

        assertFalse(
                result.isSuccess());

        assertTrue(
                result.isFailure());

        assertNull(
                result.getContent());

        assertEquals(
                "MCP server unavailable",
                result.getErrorMessage());
    }

    @Test
    void shouldUseExceptionClassNameWhenMcpFailureMessageIsBlank() {

        ToolAction action = createAction(
                "劳动合同解除条件");

        when(
                toolCallback.call(
                        anyString()))
                .thenThrow(
                        new IllegalStateException());

        ToolExecutionResult result = tool.execute(
                action);

        assertTrue(
                result.isFailure());

        assertEquals(
                "IllegalStateException",
                result.getErrorMessage());
    }

    @Test
    void shouldFailWhenMcpToolReturnsNull() {

        ToolAction action = createAction(
                "劳动合同解除条件");

        when(
                toolCallback.call(
                        anyString()))
                .thenReturn(
                        null);

        ToolExecutionResult result = tool.execute(
                action);

        assertTrue(
                result.isFailure());

        assertEquals(
                "MCP tool returned empty result",
                result.getErrorMessage());
    }

    @Test
    void shouldFailWhenMcpToolReturnsBlankResult() {

        ToolAction action = createAction(
                "劳动合同解除条件");

        when(
                toolCallback.call(
                        anyString()))
                .thenReturn(
                        "   ");

        ToolExecutionResult result = tool.execute(
                action);

        assertTrue(
                result.isFailure());

        assertEquals(
                "MCP tool returned empty result",
                result.getErrorMessage());
    }

    @Test
    void shouldRejectNullAction() {

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> tool.execute(
                        null));

        assertEquals(
                "ToolAction must not be null",
                exception.getMessage());
    }

    @Test
    void shouldRejectActionForDifferentTool() {

        ToolAction action = ToolAction.of(
                "task-1",
                "readDocument",
                Map.of());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tool.execute(
                        action));

        assertEquals(
                "ToolAction is not intended for searchLegalKnowledge: readDocument",
                exception.getMessage());
    }

    @Test
    void shouldFailFastWhenMcpToolCannotBeDiscovered() {

        ToolDefinition anotherDefinition = mock(
                ToolDefinition.class);

        ToolCallback anotherCallback = mock(
                ToolCallback.class);

        when(
                anotherDefinition.name())
                .thenReturn(
                        "anotherTool");

        when(
                anotherCallback.getToolDefinition())
                .thenReturn(
                        anotherDefinition);

        when(
                toolCallbackProvider.getToolCallbacks())
                .thenReturn(
                        new ToolCallback[] {
                                anotherCallback
                        });

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> new McpLegalKnowledgeTool(
                        toolCallbackProvider,
                        objectMapper));

        assertEquals(
                "MCP tool not found: searchLegalKnowledge",
                exception.getMessage());
    }

    @Test
    void shouldRejectNullToolCallbackProvider() {

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new McpLegalKnowledgeTool(
                        null,
                        objectMapper));

        assertEquals(
                "toolCallbackProvider must not be null",
                exception.getMessage());
    }

    @Test
    void shouldRejectNullObjectMapper() {

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new McpLegalKnowledgeTool(
                        toolCallbackProvider,
                        null));

        assertEquals(
                "objectMapper must not be null",
                exception.getMessage());
    }

    @Test
    void shouldNormalizeMcpTextContentResult() {

        ToolAction action = createAction(
                "违法解除劳动合同");

        when(
                toolCallback.call(
                        anyString()))
                .thenReturn(
                        """
                                [{"text":"劳动合同法第八十七条规定违法解除应支付赔偿金"}]
                                """);

        ToolExecutionResult result = tool.execute(
                action);

        assertTrue(
                result.isSuccess());

        assertEquals(
                "劳动合同法第八十七条规定违法解除应支付赔偿金",
                result.getContent());
    }

    @Test
    void shouldMergeMultipleMcpTextContents() {

        ToolAction action = createAction(
                "违法解除劳动合同");

        when(
                toolCallback.call(
                        anyString()))
                .thenReturn(
                        """
                                [
                                  {"text":"第一段"},
                                  {"text":"第二段"}
                                ]
                                """);

        ToolExecutionResult result = tool.execute(
                action);

        assertTrue(
                result.isSuccess());

        assertEquals(
                "第一段"
                        + System.lineSeparator()
                        + "第二段",
                result.getContent());
    }

    private ToolAction createAction(
            String legalQuestion) {

        return ToolAction.of(
                "task-1",
                LegalKnowledgeTool.TOOL_NAME,
                Map.of(
                        LegalKnowledgeTool.LEGAL_QUESTION_ARGUMENT,
                        legalQuestion));
    }
}