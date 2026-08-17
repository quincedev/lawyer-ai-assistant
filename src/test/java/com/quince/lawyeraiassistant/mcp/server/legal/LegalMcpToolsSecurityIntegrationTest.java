package com.quince.lawyeraiassistant.mcp.server.legal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.quince.lawyeraiassistant.agent.tool.legal.LegalToolContract;
import com.quince.lawyeraiassistant.retrieval.formatter.LegalRetrievalResultFormatter;
import com.quince.lawyeraiassistant.retrieval.model.RetrieverContext;
import com.quince.lawyeraiassistant.retrieval.orchestration.RetrievalOrchestrator;
import com.quince.lawyeraiassistant.security.guardrail.exception.McpToolSecurityViolationException;
import com.quince.lawyeraiassistant.security.mcp.McpToolSecurityResult;
import com.quince.lawyeraiassistant.security.mcp.McpToolSecurityService;
import com.quince.lawyeraiassistant.security.SecurityTest;
import com.quince.lawyeraiassistant.security.audit.SecurityAuditLogger;

// 按你项目真实 package 导入：
// RetrievalOrchestrator
// LegalRetrievalResultFormatter
// LegalMcpTools

@SecurityTest
class LegalMcpToolsSecurityIntegrationTest {

        @Test
        void shouldNotExecuteRetrievalWhenMcpSecurityDenies() {

                RetrievalOrchestrator retrievalOrchestrator = mock(RetrievalOrchestrator.class);

                LegalRetrievalResultFormatter resultFormatter = mock(LegalRetrievalResultFormatter.class);

                McpToolSecurityService securityService = mock(McpToolSecurityService.class);

                SecurityAuditLogger securityAuditLogger = mock(SecurityAuditLogger.class);

                when(securityService.evaluate(
                                LegalToolContract.SEARCH_LEGAL_KNOWLEDGE,
                                java.util.Map.of(
                                                "legalQuestion",
                                                "劳动合同问题")))
                                .thenReturn(
                                                McpToolSecurityResult.deny(
                                                                LegalToolContract.SEARCH_LEGAL_KNOWLEDGE,
                                                                "testPolicy",
                                                                "Denied"));

                LegalMcpTools tools = new LegalMcpTools(
                                retrievalOrchestrator,
                                resultFormatter,
                                securityService,
                                securityAuditLogger);

                assertThrows(
                                McpToolSecurityViolationException.class,
                                () -> tools.searchLegalKnowledge(
                                                "劳动合同问题"));

                verify(
                                retrievalOrchestrator,
                                never())
                                .retrieve(any());
        }

        @Test
        void shouldExecuteRetrievalWhenMcpSecurityAllows() {

                RetrievalOrchestrator retrievalOrchestrator = mock(RetrievalOrchestrator.class);

                LegalRetrievalResultFormatter resultFormatter = mock(LegalRetrievalResultFormatter.class);

                McpToolSecurityService securityService = mock(McpToolSecurityService.class);

                SecurityAuditLogger securityAuditLogger = mock(SecurityAuditLogger.class);

                when(securityService.evaluate(
                                LegalToolContract.SEARCH_LEGAL_KNOWLEDGE,
                                java.util.Map.of(
                                                "legalQuestion",
                                                "劳动合同问题")))
                                .thenReturn(
                                                McpToolSecurityResult.allow(
                                                                LegalToolContract.SEARCH_LEGAL_KNOWLEDGE,
                                                                "testPolicy"));

                RetrieverContext context = mock(RetrieverContext.class);

                when(retrievalOrchestrator.retrieve(
                                "劳动合同问题"))
                                .thenReturn(context);

                when(resultFormatter.format(
                                context))
                                .thenReturn(
                                                "劳动合同法律资料");

                LegalMcpTools tools = new LegalMcpTools(
                                retrievalOrchestrator,
                                resultFormatter,
                                securityService,
                                securityAuditLogger);

                String result = tools.searchLegalKnowledge(
                                "劳动合同问题");

                assertEquals(
                                "劳动合同法律资料",
                                result);

                verify(retrievalOrchestrator)
                                .retrieve(
                                                "劳动合同问题");
        }
}