package com.quince.lawyeraiassistant.retrieval.orchestration;

import com.quince.lawyeraiassistant.query.model.QueryContext;
import com.quince.lawyeraiassistant.query.pipeline.QueryPipeline;
import com.quince.lawyeraiassistant.retrieval.model.RetrieverContext;
import com.quince.lawyeraiassistant.retrieval.pipeline.RetrieverPipeline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RetrievalOrchestratorTest {

        private QueryPipeline queryPipeline;

        private RetrieverPipeline retrieverPipeline;

        private RetrievalOrchestrator orchestrator;

        @BeforeEach
        void setUp() {
                queryPipeline = mock(QueryPipeline.class);
                retrieverPipeline = mock(RetrieverPipeline.class);

                orchestrator = new RetrievalOrchestrator(
                                queryPipeline,
                                retrieverPipeline);
        }

        @Test
        void shouldExecuteQueryPipelineBeforeRetrieverPipeline() {
                QueryContext transformedQueryContext = QueryContext.builder()
                                .question(
                                                "老板把我开了合法吗？")
                                .rewriteQuery(
                                                "违法解除劳动合同是否合法")
                                .build();

                when(
                                queryPipeline.execute(
                                                org.mockito.ArgumentMatchers.any(
                                                                QueryContext.class)))
                                .thenReturn(transformedQueryContext);

                RetrieverContext expectedResult = RetrieverContext.builder()
                                .queryContext(
                                                transformedQueryContext)
                                .documents(
                                                List.of(
                                                                new Document(
                                                                                "劳动合同法第三十九条")))
                                .build();

                when(
                                retrieverPipeline.retrieve(
                                                org.mockito.ArgumentMatchers.any(
                                                                RetrieverContext.class)))
                                .thenReturn(expectedResult);

                RetrieverContext actualResult = orchestrator.retrieve(
                                "老板把我开了合法吗？");

                assertSame(expectedResult, actualResult);

                verify(queryPipeline).execute(
                                org.mockito.ArgumentMatchers.any(
                                                QueryContext.class));

                verify(retrieverPipeline).retrieve(
                                org.mockito.ArgumentMatchers.any(
                                                RetrieverContext.class));
        }

        @Test
        void shouldCreateInitialQueryContextFromRequest() {
                ArgumentCaptor<QueryContext> queryCaptor = ArgumentCaptor.forClass(
                                QueryContext.class);

                QueryContext transformedQueryContext = QueryContext.builder()
                                .question("那赔偿呢？")
                                .conversationId(
                                                "conversation-001")
                                .rewriteQuery(
                                                "解除劳动合同经济补偿")
                                .build();

                when(
                                queryPipeline.execute(
                                                org.mockito.ArgumentMatchers.any(
                                                                QueryContext.class)))
                                .thenReturn(transformedQueryContext);

                RetrieverContext expectedResult = RetrieverContext.from(
                                transformedQueryContext);

                when(
                                retrieverPipeline.retrieve(
                                                org.mockito.ArgumentMatchers.any(
                                                                RetrieverContext.class)))
                                .thenReturn(expectedResult);

                orchestrator.retrieve(
                                "  那赔偿呢？  ",
                                "  conversation-001  ");

                verify(queryPipeline).execute(
                                queryCaptor.capture());

                QueryContext initialContext = queryCaptor.getValue();

                assertEquals(
                                "那赔偿呢？",
                                initialContext.getQuestion());

                assertEquals(
                                "conversation-001",
                                initialContext.getConversationId());
        }

        @Test
        void shouldPassTransformedQueryContextToRetrieverPipeline() {
                QueryContext transformedQueryContext = QueryContext.builder()
                                .question(
                                                "老板不给工资怎么办？")
                                .rewriteQuery(
                                                "拖欠劳动报酬的法律救济")
                                .build();

                when(
                                queryPipeline.execute(
                                                org.mockito.ArgumentMatchers.any(
                                                                QueryContext.class)))
                                .thenReturn(transformedQueryContext);

                ArgumentCaptor<RetrieverContext> retrieverCaptor = ArgumentCaptor.forClass(
                                RetrieverContext.class);

                RetrieverContext expectedResult = RetrieverContext.from(
                                transformedQueryContext);

                when(
                                retrieverPipeline.retrieve(
                                                org.mockito.ArgumentMatchers.any(
                                                                RetrieverContext.class)))
                                .thenReturn(expectedResult);

                orchestrator.retrieve(
                                "老板不给工资怎么办？");

                verify(retrieverPipeline).retrieve(
                                retrieverCaptor.capture());

                RetrieverContext initialRetrieverContext = retrieverCaptor.getValue();

                assertSame(
                                transformedQueryContext,
                                initialRetrieverContext
                                                .getQueryContext());

                assertEquals(
                                "拖欠劳动报酬的法律救济",
                                initialRetrieverContext
                                                .effectiveQuery());

                assertEquals(
                                0,
                                initialRetrieverContext
                                                .documentCount());
        }

        @Test
        void shouldReturnDocumentsProducedByRetrieverPipeline() {
                QueryContext transformedQueryContext = QueryContext.builder()
                                .question(
                                                "劳动合同解除需要赔偿吗？")
                                .rewriteQuery(
                                                "解除劳动合同经济补偿规定")
                                .build();

                when(
                                queryPipeline.execute(
                                                org.mockito.ArgumentMatchers.any(
                                                                QueryContext.class)))
                                .thenReturn(transformedQueryContext);

                RetrieverContext retrievedContext = RetrieverContext.builder()
                                .queryContext(
                                                transformedQueryContext)
                                .documents(
                                                List.of(
                                                                new Document(
                                                                                "劳动合同法第四十六条"),
                                                                new Document(
                                                                                "劳动合同法第四十七条")))
                                .build();

                when(
                                retrieverPipeline.retrieve(
                                                org.mockito.ArgumentMatchers.any(
                                                                RetrieverContext.class)))
                                .thenReturn(retrievedContext);

                RetrieverContext result = orchestrator.retrieve(
                                "劳动合同解除需要赔偿吗？");

                assertEquals(2, result.documentCount());

                assertEquals(
                                "解除劳动合同经济补偿规定",
                                result.effectiveQuery());

                assertEquals(
                                "劳动合同解除需要赔偿吗？",
                                result.getQueryContext()
                                                .getQuestion());
        }

        @Test
        void shouldPropagateQueryPipelineException() {
                IllegalStateException expectedException = new IllegalStateException(
                                "Query rewrite failed");

                when(
                                queryPipeline.execute(
                                                org.mockito.ArgumentMatchers.any(
                                                                QueryContext.class)))
                                .thenThrow(expectedException);

                IllegalStateException actualException = assertThrows(
                                IllegalStateException.class,
                                () -> orchestrator.retrieve(
                                                "测试问题"));

                assertSame(
                                expectedException,
                                actualException);
        }

        @Test
        void shouldNotCallRetrieverWhenQueryPipelineFails() {
                when(
                                queryPipeline.execute(
                                                org.mockito.ArgumentMatchers.any(
                                                                QueryContext.class)))
                                .thenThrow(
                                                new IllegalStateException(
                                                                "Query rewrite failed"));

                assertThrows(
                                IllegalStateException.class,
                                () -> orchestrator.retrieve(
                                                "测试问题"));

                verify(
                                retrieverPipeline,
                                org.mockito.Mockito.never()).retrieve(
                                                org.mockito.ArgumentMatchers.any(
                                                                RetrieverContext.class));
        }

        @Test
        void shouldPropagateRetrieverPipelineException() {
                QueryContext transformedQueryContext = QueryContext.from(
                                "劳动合同问题");

                when(
                                queryPipeline.execute(
                                                org.mockito.ArgumentMatchers.any(
                                                                QueryContext.class)))
                                .thenReturn(transformedQueryContext);

                IllegalStateException expectedException = new IllegalStateException(
                                "Vector search failed");

                when(
                                retrieverPipeline.retrieve(
                                                org.mockito.ArgumentMatchers.any(
                                                                RetrieverContext.class)))
                                .thenThrow(expectedException);

                IllegalStateException actualException = assertThrows(
                                IllegalStateException.class,
                                () -> orchestrator.retrieve(
                                                "劳动合同问题"));

                assertSame(
                                expectedException,
                                actualException);
        }

        @Test
        void shouldThrowExceptionWhenQueryPipelineReturnsNull() {
                when(
                                queryPipeline.execute(
                                                org.mockito.ArgumentMatchers.any(
                                                                QueryContext.class)))
                                .thenReturn(null);

                IllegalStateException exception = assertThrows(
                                IllegalStateException.class,
                                () -> orchestrator.retrieve(
                                                "测试问题"));

                assertEquals(
                                "QueryPipeline must not return null",
                                exception.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenRetrieverPipelineReturnsNull() {
                QueryContext queryContext = QueryContext.from(
                                "测试问题");

                when(
                                queryPipeline.execute(
                                                org.mockito.ArgumentMatchers.any(
                                                                QueryContext.class)))
                                .thenReturn(queryContext);

                when(
                                retrieverPipeline.retrieve(
                                                org.mockito.ArgumentMatchers.any(
                                                                RetrieverContext.class)))
                                .thenReturn(null);

                IllegalStateException exception = assertThrows(
                                IllegalStateException.class,
                                () -> orchestrator.retrieve(
                                                "测试问题"));

                assertEquals(
                                "RetrieverPipeline must not return null",
                                exception.getMessage());
        }

        @Test
        void shouldRejectNullConstructorDependencies() {
                NullPointerException queryPipelineException = assertThrows(
                                NullPointerException.class,
                                () -> new RetrievalOrchestrator(
                                                null,
                                                retrieverPipeline));

                assertEquals(
                                "queryPipeline must not be null",
                                queryPipelineException.getMessage());

                NullPointerException retrieverPipelineException = assertThrows(
                                NullPointerException.class,
                                () -> new RetrievalOrchestrator(
                                                queryPipeline,
                                                null));

                assertEquals(
                                "retrieverPipeline must not be null",
                                retrieverPipelineException.getMessage());
        }

        @Test
        void shouldPropagateTenantIdIntoRetrieverPipeline() {

                QueryContext transformed = QueryContext.from(
                                "劳动合同问题");

                when(
                                queryPipeline.execute(
                                                org.mockito.ArgumentMatchers.any(
                                                                QueryContext.class)))
                                .thenReturn(
                                                transformed);

                ArgumentCaptor<RetrieverContext> captor = ArgumentCaptor.forClass(
                                RetrieverContext.class);

                when(
                                retrieverPipeline.retrieve(
                                                org.mockito.ArgumentMatchers.any(
                                                                RetrieverContext.class)))
                                .thenAnswer(
                                                invocation -> invocation.getArgument(
                                                                0));

                orchestrator.retrieveForTenant(
                                "劳动合同问题",
                                "tenant-a");

                verify(
                                retrieverPipeline)
                                .retrieve(
                                                captor.capture());

                RetrieverContext context = captor.getValue();

                assertTrue(
                                context.hasTenantId());

                assertEquals(
                                "tenant-a",
                                context.requireTenantId());
        }
        
}