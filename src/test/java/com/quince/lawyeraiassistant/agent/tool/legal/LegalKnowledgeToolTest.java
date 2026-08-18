package com.quince.lawyeraiassistant.agent.tool.legal;

import com.quince.lawyeraiassistant.agent.model.ToolAction;
import com.quince.lawyeraiassistant.agent.model.ToolExecutionResult;
import com.quince.lawyeraiassistant.agent.tool.ToolExecutionContext;
import com.quince.lawyeraiassistant.query.model.QueryContext;
import com.quince.lawyeraiassistant.retrieval.formatter.LegalRetrievalResultFormatter;
import com.quince.lawyeraiassistant.retrieval.model.RetrieverContext;
import com.quince.lawyeraiassistant.retrieval.orchestration.RetrievalOrchestrator;
import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.security.identity.UserRole;
import com.quince.lawyeraiassistant.security.tenant.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LegalKnowledgeToolTest {

        private RetrievalOrchestrator retrievalOrchestrator;

        private LegalKnowledgeTool tool;

        private LegalRetrievalResultFormatter resultFormatter;

        @BeforeEach
        void setUp() {

                retrievalOrchestrator = mock(
                                RetrievalOrchestrator.class);

                resultFormatter = new LegalRetrievalResultFormatter();

                tool = new LegalKnowledgeTool(
                                retrievalOrchestrator,
                                resultFormatter);
        }

        @Test
        void shouldReturnExpectedToolName() {

                assertEquals(
                                "searchLegalKnowledge",
                                tool.name());
        }

        @Test
        void shouldRetrieveForTenantWhenTrustedContextIsPresent() {
                String question = "tenant question";
                ToolAction action = ToolAction.of(
                                "task-tenant",
                                LegalKnowledgeTool.TOOL_NAME,
                                Map.of(LegalKnowledgeTool.LEGAL_QUESTION_ARGUMENT, question));
                TenantContext tenant = new TenantContext(
                                "tenant-a",
                                "user-a",
                                "lawyer-a",
                                Set.of(UserRole.LAWYER));
                ToolExecutionContext executionContext = ToolExecutionContext.from(
                                AgentContext.builder().goal("research").tenantContext(tenant).build());
                RetrieverContext retrievalContext = RetrieverContext.from(QueryContext.from(question));
                when(retrievalOrchestrator.retrieveForTenant(question, "tenant-a"))
                                .thenReturn(retrievalContext);

                ToolExecutionResult result = tool.execute(executionContext, action);

                assertTrue(result.isSuccess());
                verify(retrievalOrchestrator).retrieveForTenant(question, "tenant-a");
                verify(retrievalOrchestrator, never()).retrieve(question);
        }

        @Test
        void shouldRetrieveLegalKnowledge() {

                ToolAction action = ToolAction.of(
                                "task-4",
                                LegalKnowledgeTool.TOOL_NAME,
                                Map.of(
                                                LegalKnowledgeTool.LEGAL_QUESTION_ARGUMENT,
                                                "劳动合同违法解除的法律责任"));

                QueryContext queryContext = QueryContext.builder()
                                .question(
                                                "劳动合同违法解除的法律责任")
                                .rewriteQuery(
                                                "违法解除劳动合同赔偿责任")
                                .build();

                Document firstDocument = new Document(
                                "doc-1",
                                "第八十七条 用人单位违法解除或者终止劳动合同的，应当支付赔偿金。",
                                Map.of(
                                                "file_name",
                                                "Labor-Contract-Law.pdf",
                                                "page_number",
                                                24));

                Document secondDocument = new Document(
                                "doc-2",
                                "第四十七条规定了经济补偿的计算标准。",
                                Map.of(
                                                "file_name",
                                                "Labor-Contract-Law.pdf",
                                                "page_number",
                                                14));

                RetrieverContext retrievalContext = RetrieverContext.builder()
                                .queryContext(
                                                queryContext)
                                .documents(
                                                List.of(
                                                                firstDocument,
                                                                secondDocument))
                                .build();

                when(
                                retrievalOrchestrator.retrieve(
                                                "劳动合同违法解除的法律责任"))
                                .thenReturn(
                                                retrievalContext);

                ToolExecutionResult result = tool.execute(
                                action);

                assertTrue(
                                result.isSuccess());

                assertFalse(
                                result.isFailure());

                assertNull(
                                result.getErrorMessage());

                assertTrue(
                                result.getContent()
                                                .contains(
                                                                "有效检索问题：违法解除劳动合同赔偿责任"));

                assertTrue(
                                result.getContent()
                                                .contains(
                                                                "检索文档数量：2"));

                assertTrue(
                                result.getContent()
                                                .contains(
                                                                "第八十七条"));

                assertTrue(
                                result.getContent()
                                                .contains(
                                                                "第四十七条"));

                assertTrue(
                                result.getContent()
                                                .contains(
                                                                "Labor-Contract-Law.pdf"));

                verify(
                                retrievalOrchestrator).retrieve(
                                                "劳动合同违法解除的法律责任");
        }

        @Test
        void shouldTrimLegalQuestionBeforeRetrieval() {

                ToolAction action = ToolAction.of(
                                "task-1",
                                LegalKnowledgeTool.TOOL_NAME,
                                Map.of(
                                                LegalKnowledgeTool.LEGAL_QUESTION_ARGUMENT,
                                                "  劳动合同解除条件  "));

                RetrieverContext retrievalContext = RetrieverContext.from(
                                QueryContext.from(
                                                "劳动合同解除条件"));

                when(
                                retrievalOrchestrator.retrieve(
                                                "劳动合同解除条件"))
                                .thenReturn(
                                                retrievalContext);

                ToolExecutionResult result = tool.execute(
                                action);

                assertTrue(
                                result.isSuccess());

                verify(
                                retrievalOrchestrator).retrieve(
                                                "劳动合同解除条件");
        }

        @Test
        void shouldReturnSuccessfulResultWhenNoDocumentsFound() {

                ToolAction action = createAction(
                                "一个知识库中不存在的问题");

                RetrieverContext retrievalContext = RetrieverContext.from(
                                QueryContext.from(
                                                "一个知识库中不存在的问题"));

                when(
                                retrievalOrchestrator.retrieve(
                                                "一个知识库中不存在的问题"))
                                .thenReturn(
                                                retrievalContext);

                ToolExecutionResult result = tool.execute(
                                action);

                assertTrue(
                                result.isSuccess());

                assertEquals(
                                "未检索到与当前法律问题相关的知识。",
                                result.getContent());
        }

        @Test
        void shouldConvertRetrievalExceptionToFailedResult() {

                ToolAction action = createAction(
                                "劳动合同解除条件");

                when(
                                retrievalOrchestrator.retrieve(
                                                "劳动合同解除条件"))
                                .thenThrow(
                                                new IllegalStateException(
                                                                "VectorStore unavailable"));

                ToolExecutionResult result = tool.execute(
                                action);

                assertFalse(
                                result.isSuccess());

                assertTrue(
                                result.isFailure());

                assertNull(
                                result.getContent());

                assertEquals(
                                "VectorStore unavailable",
                                result.getErrorMessage());
        }

        @Test
        void shouldUseExceptionClassNameWhenFailureMessageIsBlank() {

                ToolAction action = createAction(
                                "劳动合同解除条件");

                when(
                                retrievalOrchestrator.retrieve(
                                                "劳动合同解除条件"))
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
        void shouldRejectNullAction() {

                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> tool.execute(null));

                assertEquals(
                                "ToolAction must not be null",
                                exception.getMessage());

                verify(
                                retrievalOrchestrator,
                                never()).retrieve(
                                                org.mockito.ArgumentMatchers
                                                                .anyString());
        }

        @Test
        void shouldRejectActionForDifferentTool() {

                ToolAction action = ToolAction.of(
                                "task-1",
                                "readDocument",
                                Map.of(
                                                LegalKnowledgeTool.LEGAL_QUESTION_ARGUMENT,
                                                "劳动合同解除"));

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> tool.execute(
                                                action));

                assertEquals(
                                "ToolAction is not intended for searchLegalKnowledge: readDocument",
                                exception.getMessage());

                verify(
                                retrievalOrchestrator,
                                never()).retrieve(
                                                org.mockito.ArgumentMatchers
                                                                .anyString());
        }

        @Test
        void shouldRejectMissingLegalQuestion() {

                ToolAction action = ToolAction.of(
                                "task-1",
                                LegalKnowledgeTool.TOOL_NAME,
                                Map.of(
                                                "wrongArgument",
                                                "劳动合同解除"));

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> tool.execute(
                                                action));

                assertEquals(
                                "Missing required tool argument: legalQuestion",
                                exception.getMessage());

                verify(
                                retrievalOrchestrator,
                                never()).retrieve(
                                                org.mockito.ArgumentMatchers
                                                                .anyString());
        }

        @Test
        void shouldRejectBlankLegalQuestion() {

                ToolAction action = ToolAction.of(
                                "task-1",
                                LegalKnowledgeTool.TOOL_NAME,
                                Map.of(
                                                LegalKnowledgeTool.LEGAL_QUESTION_ARGUMENT,
                                                "   "));

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> tool.execute(
                                                action));

                assertEquals(
                                "Tool argument legalQuestion must not be blank",
                                exception.getMessage());

                verify(
                                retrievalOrchestrator,
                                never()).retrieve(
                                                org.mockito.ArgumentMatchers
                                                                .anyString());
        }

        @Test
        void shouldRejectNullRetrievalResult() {

                ToolAction action = createAction(
                                "劳动合同解除条件");

                when(
                                retrievalOrchestrator.retrieve(
                                                "劳动合同解除条件"))
                                .thenReturn(
                                                null);

                ToolExecutionResult result = tool.execute(
                                action);

                assertTrue(
                                result.isFailure());

                assertEquals(
                                "RetrievalOrchestrator must not return null",
                                result.getErrorMessage());
        }

        @Test
        void shouldRejectNullRetrievalOrchestrator() {

                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> new LegalKnowledgeTool(
                                                null,
                                                resultFormatter));

                assertEquals(
                                "retrievalOrchestrator must not be null",
                                exception.getMessage());
        }

        @Test
        void shouldRejectNullResultFormatter() {

                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> new LegalKnowledgeTool(
                                                retrievalOrchestrator,
                                                null));

                assertEquals(
                                "resultFormatter must not be null",
                                exception.getMessage());
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
