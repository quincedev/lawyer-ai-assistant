package com.quince.lawyeraiassistant.mcp.server.legal;

import com.quince.lawyeraiassistant.query.model.QueryContext;
import com.quince.lawyeraiassistant.retrieval.formatter.LegalRetrievalResultFormatter;
import com.quince.lawyeraiassistant.retrieval.model.RetrieverContext;
import com.quince.lawyeraiassistant.retrieval.orchestration.RetrievalOrchestrator;
import com.quince.lawyeraiassistant.security.SecurityTest;
import com.quince.lawyeraiassistant.security.audit.SecurityAuditLogger;
import com.quince.lawyeraiassistant.security.mcp.McpToolSecurityResult;
import com.quince.lawyeraiassistant.security.mcp.McpToolSecurityService;
import com.quince.lawyeraiassistant.security.mcp.tenant.McpTenantExecutionTokenService;
import com.quince.lawyeraiassistant.security.identity.UserRole;
import com.quince.lawyeraiassistant.security.tenant.TenantContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LegalMcpToolsTest {

        private RetrievalOrchestrator retrievalOrchestrator;

        private LegalRetrievalResultFormatter resultFormatter;

        private LegalMcpTools legalMcpTools;

        private McpToolSecurityService securityService;

        private SecurityAuditLogger securityAuditLogger;

        private McpTenantExecutionTokenService tenantExecutionTokenService;

        @BeforeEach
        void setUp() {

                retrievalOrchestrator = mock(
                                RetrievalOrchestrator.class);

                resultFormatter = new LegalRetrievalResultFormatter();

                securityService = mock(
                                McpToolSecurityService.class);

                securityAuditLogger = mock(SecurityAuditLogger.class);

                tenantExecutionTokenService = mock(McpTenantExecutionTokenService.class);

                when(
                                securityService.evaluate(
                                                org.mockito.ArgumentMatchers.anyString(),
                                                org.mockito.ArgumentMatchers.anyMap()))
                                .thenAnswer(
                                                invocation -> McpToolSecurityResult.allow(
                                                                invocation.getArgument(0),
                                                                "testMcpSecurity"));

                legalMcpTools = new LegalMcpTools(
                                retrievalOrchestrator,
                                resultFormatter,
                                securityService,
                                securityAuditLogger,
                                tenantExecutionTokenService);
        }

        @Test
        void shouldSearchLegalKnowledge() {

                QueryContext queryContext = QueryContext.builder()
                                .question(
                                                "违法解除劳动合同有什么责任")
                                .rewriteQuery(
                                                "违法解除劳动合同赔偿责任")
                                .build();

                Document document = new Document(
                                "doc-1",
                                "第八十七条规定，用人单位违法解除劳动合同，应当支付赔偿金。",
                                Map.of(
                                                "file_name",
                                                "Labor-Contract-Law.pdf",
                                                "page_number",
                                                24));

                RetrieverContext retrievalContext = RetrieverContext.builder()
                                .queryContext(
                                                queryContext)
                                .documents(
                                                List.of(
                                                                document))
                                .build();

                when(
                                retrievalOrchestrator.retrieve(
                                                "违法解除劳动合同有什么责任"))
                                .thenReturn(
                                                retrievalContext);

                String result = legalMcpTools.searchLegalKnowledge(
                                "违法解除劳动合同有什么责任");

                assertTrue(
                                result.contains(
                                                "有效检索问题：违法解除劳动合同赔偿责任"));

                assertTrue(
                                result.contains(
                                                "第八十七条"));

                assertTrue(
                                result.contains(
                                                "Labor-Contract-Law.pdf"));

                verify(
                                retrievalOrchestrator)
                                .retrieve(
                                                "违法解除劳动合同有什么责任");
        }

        @SecurityTest
        @Test
        void shouldRetrieveForTenantWithValidExecutionToken() {
                String question = "tenant question";
                TenantContext tenant = new TenantContext(
                                "tenant-a",
                                "user-a",
                                "lawyer-a",
                                Set.of(UserRole.LAWYER));
                RetrieverContext retrievalContext = RetrieverContext.from(QueryContext.from(question));
                when(tenantExecutionTokenService.verify("valid-token")).thenReturn(tenant);
                when(retrievalOrchestrator.retrieveForTenant(question, "tenant-a"))
                                .thenReturn(retrievalContext);

                legalMcpTools.searchLegalKnowledge(question, "valid-token");

                verify(retrievalOrchestrator).retrieveForTenant(question, "tenant-a");
                verify(retrievalOrchestrator, never()).retrieve(question);
        }

        @SecurityTest
        @Test
        void shouldUseSharedOnlyRetrievalWithoutExecutionToken() {
                String question = "shared question";
                when(retrievalOrchestrator.retrieve(question))
                                .thenReturn(RetrieverContext.from(QueryContext.from(question)));

                legalMcpTools.searchLegalKnowledge(question, null);

                verify(retrievalOrchestrator).retrieve(question);
                verify(retrievalOrchestrator, never()).retrieveForTenant(
                                org.mockito.ArgumentMatchers.anyString(),
                                org.mockito.ArgumentMatchers.anyString());
        }

        @SecurityTest
        @Test
        void shouldNotRetrieveWhenExecutionTokenIsInvalid() {
                when(tenantExecutionTokenService.verify("invalid-token"))
                                .thenThrow(new IllegalArgumentException("invalid token"));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> legalMcpTools.searchLegalKnowledge("question", "invalid-token"));

                verify(retrievalOrchestrator, never()).retrieve(
                                org.mockito.ArgumentMatchers.anyString());
                verify(retrievalOrchestrator, never()).retrieveForTenant(
                                org.mockito.ArgumentMatchers.anyString(),
                                org.mockito.ArgumentMatchers.anyString());
        }

        @Test
        void shouldTrimLegalQuestionBeforeRetrieval() {

                RetrieverContext retrievalContext = RetrieverContext.from(
                                QueryContext.from(
                                                "劳动合同解除条件"));

                when(
                                retrievalOrchestrator.retrieve(
                                                "劳动合同解除条件"))
                                .thenReturn(
                                                retrievalContext);

                String result = legalMcpTools.searchLegalKnowledge(
                                "   劳动合同解除条件   ");

                assertEquals(
                                "未检索到与当前法律问题相关的知识。",
                                result);

                verify(
                                retrievalOrchestrator)
                                .retrieve(
                                                "劳动合同解除条件");
        }

        @Test
        void shouldReturnNoKnowledgeMessageWhenNoDocumentsFound() {

                RetrieverContext retrievalContext = RetrieverContext.from(
                                QueryContext.from(
                                                "不存在的法律问题"));

                when(
                                retrievalOrchestrator.retrieve(
                                                "不存在的法律问题"))
                                .thenReturn(
                                                retrievalContext);

                String result = legalMcpTools.searchLegalKnowledge(
                                "不存在的法律问题");

                assertEquals(
                                "未检索到与当前法律问题相关的知识。",
                                result);
        }

        @Test
        void shouldPropagateRetrievalException() {

                when(
                                retrievalOrchestrator.retrieve(
                                                "劳动合同解除条件"))
                                .thenThrow(
                                                new IllegalStateException(
                                                                "VectorStore unavailable"));

                IllegalStateException exception = assertThrows(
                                IllegalStateException.class,
                                () -> legalMcpTools.searchLegalKnowledge(
                                                "劳动合同解除条件"));

                assertEquals(
                                "VectorStore unavailable",
                                exception.getMessage());
        }

        @Test
        void shouldRejectNullRetrievalResult() {

                when(
                                retrievalOrchestrator.retrieve(
                                                "劳动合同解除条件"))
                                .thenReturn(
                                                null);

                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> legalMcpTools.searchLegalKnowledge(
                                                "劳动合同解除条件"));

                assertEquals(
                                "RetrievalOrchestrator must not return null",
                                exception.getMessage());
        }

        @Test
        void shouldRejectNullLegalQuestion() {

                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> legalMcpTools.searchLegalKnowledge(
                                                null));

                assertEquals(
                                "Legal question must not be null",
                                exception.getMessage());

                verify(
                                retrievalOrchestrator,
                                never())
                                .retrieve(
                                                org.mockito.ArgumentMatchers
                                                                .anyString());
        }

        @Test
        void shouldRejectBlankLegalQuestion() {

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> legalMcpTools.searchLegalKnowledge(
                                                "   "));

                assertEquals(
                                "Legal question must not be blank",
                                exception.getMessage());

                verify(
                                retrievalOrchestrator,
                                never())
                                .retrieve(
                                                org.mockito.ArgumentMatchers
                                                                .anyString());
        }

        @Test
        void shouldRejectNullRetrievalOrchestrator() {

                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> new LegalMcpTools(
                                                null,
                                                resultFormatter,
                                                securityService,
                                                securityAuditLogger,
                                                tenantExecutionTokenService));

                assertEquals(
                                "retrievalOrchestrator must not be null",
                                exception.getMessage());
        }

        @Test
        void shouldRejectNullResultFormatter() {

                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> new LegalMcpTools(
                                                retrievalOrchestrator,
                                                null,
                                                securityService,
                                                securityAuditLogger,
                                                tenantExecutionTokenService));

                assertEquals(
                                "resultFormatter must not be null",
                                exception.getMessage());
        }

        @Test
        void shouldRejectNullMcpToolSecurityService() {

                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> new LegalMcpTools(
                                                retrievalOrchestrator,
                                                resultFormatter,
                                                null,
                                                securityAuditLogger,
                                                tenantExecutionTokenService));

                assertEquals(
                                "mcpToolSecurityService must not be null",
                                exception.getMessage());
        }
}
