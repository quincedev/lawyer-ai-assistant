package com.quince.lawyeraiassistant.agent.tool.legal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.quince.lawyeraiassistant.agent.model.ToolAction;
import com.quince.lawyeraiassistant.agent.model.ToolExecutionResult;
import com.quince.lawyeraiassistant.agent.tool.ToolExecutionContext;
import com.quince.lawyeraiassistant.agent.tool.legal.cache.LegalToolCache;
import com.quince.lawyeraiassistant.agent.tool.legal.cache.LegalToolCacheHit;
import com.quince.lawyeraiassistant.agent.tool.legal.mcp.LegalKnowledgeMcpClient;
import com.quince.lawyeraiassistant.agent.tool.legal.mcp.result.McpLegalResultProcessor;
import com.quince.lawyeraiassistant.security.guardrail.exception.McpToolResultSecurityViolationException;
import com.quince.lawyeraiassistant.security.mcp.result.McpToolResultSecurityResult;

class McpLegalKnowledgeToolTest {

    private LegalKnowledgeMcpClient mcpClient;

    private McpLegalResultProcessor resultProcessor;

    private LegalToolCache legalToolCache;

    private McpLegalKnowledgeTool tool;

    @BeforeEach
    void setUp() {

        mcpClient = mock(LegalKnowledgeMcpClient.class);

        resultProcessor = mock(McpLegalResultProcessor.class);

        legalToolCache = mock(LegalToolCache.class);

        tool = new McpLegalKnowledgeTool(
                mcpClient,
                resultProcessor,
                legalToolCache);
    }

    @Test
    void shouldReturnExpectedToolName() {

        assertEquals(
                LegalKnowledgeTool.TOOL_NAME,
                tool.name());
    }

    @Test
    void cacheHitShouldUseProcessorWithoutCallingMcp() {

        ToolAction action = action(
                "cached question");

        ToolExecutionContext executionContext = ToolExecutionContext.sharedOnly();

        when(legalToolCache.get(
                executionContext,
                action))
                .thenReturn(
                        Optional.of(
                                new LegalToolCacheHit(
                                        "cached-raw")));

        when(resultProcessor.process(
                "cached-raw"))
                .thenReturn(
                        ToolExecutionResult.success(
                                "processed"));

        ToolExecutionResult result = tool.execute(
                executionContext,
                action);

        assertTrue(
                result.isSuccess());

        assertEquals(
                "processed",
                result.getContent());

        verify(legalToolCache).get(
                executionContext,
                action);

        verify(resultProcessor).process(
                "cached-raw");

        verifyNoInteractions(
                mcpClient);

        verify(legalToolCache, never())
                .put(
                        any(),
                        any(),
                        any());
    }

    @Test
    void failedProcessingShouldNotPopulateCache() {

        ToolAction action = action(
                "failure");

        ToolExecutionContext executionContext = ToolExecutionContext.sharedOnly();

        when(legalToolCache.get(
                executionContext,
                action))
                .thenReturn(
                        Optional.empty());

        when(mcpClient.search(
                executionContext,
                action.getArguments()))
                .thenReturn(
                        "raw");

        when(resultProcessor.process(
                "raw"))
                .thenReturn(
                        ToolExecutionResult.failure(
                                "processing failed"));

        ToolExecutionResult result = tool.execute(
                executionContext,
                action);

        assertTrue(
                result.isFailure());

        verify(legalToolCache, never())
                .put(
                        any(),
                        any(),
                        any());
    }

    @Test
    void processorSecurityExceptionShouldBecomeSanitizedFailure() {

        ToolAction action = action(
                "unsafe");

        ToolExecutionContext executionContext = ToolExecutionContext.sharedOnly();

        when(legalToolCache.get(
                executionContext,
                action))
                .thenReturn(
                        Optional.empty());

        when(mcpClient.search(
                executionContext,
                action.getArguments()))
                .thenReturn(
                        "unsafe-raw");

        when(resultProcessor.process(
                "unsafe-raw"))
                .thenThrow(
                        new McpToolResultSecurityViolationException(
                                McpToolResultSecurityResult.deny(
                                        LegalKnowledgeTool.TOOL_NAME,
                                        "policy",
                                        "secret reason")));

        ToolExecutionResult result = tool.execute(
                executionContext,
                action);

        assertTrue(
                result.isFailure());

        assertEquals(
                "MCP tool result was rejected by security policy",
                result.getErrorMessage());

        verify(legalToolCache, never())
                .put(
                        any(),
                        any(),
                        any());
    }

    @Test
    void emptyMcpResultShouldFailWithoutCallingProcessor() {

        ToolAction action = action(
                "empty");

        ToolExecutionContext executionContext = ToolExecutionContext.sharedOnly();

        when(legalToolCache.get(
                executionContext,
                action))
                .thenReturn(
                        Optional.empty());

        when(mcpClient.search(
                executionContext,
                action.getArguments()))
                .thenReturn(
                        "   ");

        ToolExecutionResult result = tool.execute(
                executionContext,
                action);

        assertTrue(
                result.isFailure());

        assertEquals(
                "MCP tool returned empty result",
                result.getErrorMessage());

        verifyNoInteractions(
                resultProcessor);

        verify(legalToolCache, never())
                .put(
                        any(),
                        any(),
                        any());
    }

    @Test
    void runtimeExceptionShouldBecomeSanitizedFailure() {

        ToolAction action = action(
                "failure");

        ToolExecutionContext executionContext = ToolExecutionContext.sharedOnly();

        when(legalToolCache.get(
                executionContext,
                action))
                .thenReturn(
                        Optional.empty());

        when(mcpClient.search(
                executionContext,
                action.getArguments()))
                .thenThrow(
                        new IllegalStateException(
                                "internal MCP details"));

        ToolExecutionResult result = tool.execute(
                executionContext,
                action);

        assertTrue(
                result.isFailure());

        assertEquals(
                "MCP tool execution failed",
                result.getErrorMessage());
    }

    @Test
    void legacyExecuteShouldUseSharedOnlyContext() {

        ToolAction action = action(
                "legacy");

        when(legalToolCache.get(
                any(ToolExecutionContext.class),
                eq(action)))
                .thenReturn(
                        Optional.empty());

        when(mcpClient.search(
                any(ToolExecutionContext.class),
                eq(action.getArguments())))
                .thenReturn(
                        "raw");

        when(resultProcessor.process(
                "raw"))
                .thenReturn(
                        ToolExecutionResult.success(
                                "processed"));

        ToolExecutionResult result = tool.execute(
                action);

        assertTrue(
                result.isSuccess());

        verify(mcpClient).search(
                any(ToolExecutionContext.class),
                eq(action.getArguments()));
    }

    @Test
    void wrongToolNameShouldBeRejectedBeforeDependenciesAreCalled() {

        ToolAction action = ToolAction.of(
                "task",
                "anotherTool",
                Map.of(
                        "question",
                        "test"));

        assertThrows(
                IllegalArgumentException.class,
                () -> tool.execute(
                        action));

        verifyNoInteractions(
                mcpClient,
                resultProcessor,
                legalToolCache);
    }

    @Test
    void shouldRejectNullDependencies() {

        assertEquals(
                "mcpClient must not be null",
                assertThrows(
                        NullPointerException.class,
                        () -> new McpLegalKnowledgeTool(
                                null,
                                resultProcessor,
                                legalToolCache))
                        .getMessage());

        assertEquals(
                "resultProcessor must not be null",
                assertThrows(
                        NullPointerException.class,
                        () -> new McpLegalKnowledgeTool(
                                mcpClient,
                                null,
                                legalToolCache))
                        .getMessage());

        assertEquals(
                "legalToolCache must not be null",
                assertThrows(
                        NullPointerException.class,
                        () -> new McpLegalKnowledgeTool(
                                mcpClient,
                                resultProcessor,
                                null))
                        .getMessage());
    }

    private ToolAction action(
            String question) {

        return ToolAction.of(
                "task",
                LegalKnowledgeTool.TOOL_NAME,
                Map.of(
                        LegalKnowledgeTool.LEGAL_QUESTION_ARGUMENT,
                        question));
    }
}