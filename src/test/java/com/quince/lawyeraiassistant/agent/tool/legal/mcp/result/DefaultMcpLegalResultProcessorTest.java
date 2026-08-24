package com.quince.lawyeraiassistant.agent.tool.legal.mcp.result;

import com.quince.lawyeraiassistant.agent.model.ToolExecutionResult;
import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceContext;
import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceMetrics;
import com.quince.lawyeraiassistant.agent.tool.legal.LegalKnowledgeTool;
import com.quince.lawyeraiassistant.agent.tool.legal.evidence.LegalEvidenceCompactor;
import com.quince.lawyeraiassistant.security.audit.SecurityAuditEvent;
import com.quince.lawyeraiassistant.security.audit.SecurityAuditEventType;
import com.quince.lawyeraiassistant.security.audit.SecurityAuditLogger;
import com.quince.lawyeraiassistant.security.guardrail.exception.McpToolResultSecurityViolationException;
import com.quince.lawyeraiassistant.security.mcp.result.McpToolResultSecurityResult;
import com.quince.lawyeraiassistant.security.mcp.result.McpToolResultSecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DefaultMcpLegalResultProcessorTest {
    private McpToolResultSecurityService securityService;
    private SecurityAuditLogger auditLogger;
    private LegalEvidenceCompactor compactor;
    private AgentPerformanceContext performanceContext;
    private DefaultMcpLegalResultProcessor processor;

    @BeforeEach
    void setUp() {
        securityService = mock(McpToolResultSecurityService.class);
        auditLogger = mock(SecurityAuditLogger.class);
        compactor = mock(LegalEvidenceCompactor.class);
        performanceContext = new AgentPerformanceContext();
        processor = new DefaultMcpLegalResultProcessor(
                new ObjectMapper(), securityService, auditLogger, compactor, performanceContext);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\r\n"})
    void nullOrBlankRawResultShouldFail(String rawResult) {
        ToolExecutionResult result = processor.process(rawResult);
        assertTrue(result.isFailure());
        assertEquals("MCP tool returned empty result", result.getErrorMessage());
        verifyNoInteractions(securityService, auditLogger, compactor);
    }

    @Test
    void plainTextShouldEvaluateSecurityThenCompactAndSucceed() {
        when(securityService.evaluate(LegalKnowledgeTool.TOOL_NAME, "full evidence"))
                .thenReturn(allow());
        when(compactor.compact("full evidence")).thenReturn("compacted evidence");
        ToolExecutionResult result = processor.process("  full evidence  ");
        assertTrue(result.isSuccess());
        assertEquals("compacted evidence", result.getContent());
        InOrder order = inOrder(securityService, compactor);
        order.verify(securityService).evaluate(LegalKnowledgeTool.TOOL_NAME, "full evidence");
        order.verify(compactor).compact("full evidence");
    }

    @Test
    void mcpJsonArrayShouldConcatenateTextFields() {
        String normalized = "first" + System.lineSeparator() + "second";
        when(securityService.evaluate(LegalKnowledgeTool.TOOL_NAME, normalized)).thenReturn(allow());
        when(compactor.compact(normalized)).thenReturn(normalized);
        ToolExecutionResult result = processor.process("""
                [{"type":"text","text":" first "},{"type":"image"},{"text":"second"}]
                """);
        assertTrue(result.isSuccess());
        assertEquals(normalized, result.getContent());
    }

    @Test
    void securityDenyShouldAuditAndThrowBeforeCompaction() {
        McpToolResultSecurityResult denial = McpToolResultSecurityResult.deny(
                LegalKnowledgeTool.TOOL_NAME, "promptInjectionPolicy", "unsafe result");
        when(securityService.evaluate(LegalKnowledgeTool.TOOL_NAME, "complete unsafe evidence"))
                .thenReturn(denial);
        assertThrows(McpToolResultSecurityViolationException.class,
                () -> processor.process("complete unsafe evidence"));

        InOrder order = inOrder(securityService, auditLogger, compactor);
        order.verify(securityService).evaluate(LegalKnowledgeTool.TOOL_NAME, "complete unsafe evidence");
        ArgumentCaptor<SecurityAuditEvent> event = ArgumentCaptor.forClass(SecurityAuditEvent.class);
        order.verify(auditLogger).log(event.capture());
        verifyNoInteractions(compactor);
        assertEquals(SecurityAuditEventType.MCP_RESULT_SECURITY_REJECTED, event.getValue().type());
        assertEquals("unsafe result", event.getValue().reason());
    }

    @Test
    void blankCompactedEvidenceShouldFail() {
        when(securityService.evaluate(LegalKnowledgeTool.TOOL_NAME, "evidence")).thenReturn(allow());
        when(compactor.compact("evidence")).thenReturn("  ");
        ToolExecutionResult result = processor.process("evidence");
        assertTrue(result.isFailure());
        assertEquals("MCP tool result became empty after evidence compaction", result.getErrorMessage());
    }

    @Test
    void shorterEvidenceShouldRecordCompactionMetrics() {
        AgentPerformanceMetrics metrics = performanceContext.start();
        when(securityService.evaluate(LegalKnowledgeTool.TOOL_NAME, "1234567890")).thenReturn(allow());
        when(compactor.compact("1234567890")).thenReturn("1234");
        processor.process("1234567890");
        assertEquals(10, metrics.snapshot().evidenceOriginalChars());
        assertEquals(4, metrics.snapshot().evidenceCompactedChars());
    }

    @Test
    void invalidJsonShouldFallBackToNormalizedPlainText() {
        String invalidJson = "[{not-json}]";
        when(securityService.evaluate(LegalKnowledgeTool.TOOL_NAME, invalidJson)).thenReturn(allow());
        when(compactor.compact(invalidJson)).thenReturn(invalidJson);
        ToolExecutionResult result = processor.process("  " + invalidJson + "  ");
        assertTrue(result.isSuccess());
        assertEquals(invalidJson, result.getContent());
        verify(securityService).evaluate(LegalKnowledgeTool.TOOL_NAME, invalidJson);
    }

    private McpToolResultSecurityResult allow() {
        return McpToolResultSecurityResult.allow(LegalKnowledgeTool.TOOL_NAME, "testPolicy");
    }
}
