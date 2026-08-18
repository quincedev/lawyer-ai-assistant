package com.quince.lawyeraiassistant.retrieval.operator;

import com.quince.lawyeraiassistant.query.model.QueryContext;
import com.quince.lawyeraiassistant.rag.vector.tenant.TenantKnowledgeFilterFactory;
import com.quince.lawyeraiassistant.retrieval.model.RetrieverContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VectorSearchOperatorTest {

        private DocumentRetriever documentRetriever;

        private VectorSearchOperator operator;

        private TenantKnowledgeFilterFactory tenantKnowledgeFilterFactory;

        @BeforeEach
        void setUp() {

                documentRetriever = mock(
                                DocumentRetriever.class);

                tenantKnowledgeFilterFactory = mock(
                                TenantKnowledgeFilterFactory.class);

                when(
                                tenantKnowledgeFilterFactory
                                                .createSharedOnly())
                                .thenReturn(
                                                "knowledge_scope == 'SHARED'");

                when(
                                tenantKnowledgeFilterFactory
                                                .createForTenant(
                                                                org.mockito.ArgumentMatchers.anyString()))
                                .thenAnswer(
                                                invocation -> "tenant-filter:"
                                                                + invocation.getArgument(
                                                                                0));

                operator = new VectorSearchOperator(
                                documentRetriever,
                                tenantKnowledgeFilterFactory);
        }

        @Test
        void shouldRetrieveDocumentsUsingEffectiveQuery() {
                RetrieverContext context = RetrieverContext.from(
                                QueryContext.builder()
                                                .question(
                                                                "老板把我开了合法吗？")
                                                .rewriteQuery(
                                                                "违法解除劳动合同是否合法")
                                                .build());

                List<Document> retrievedDocuments = List.of(
                                new Document(
                                                "劳动合同法第三十九条"),
                                new Document(
                                                "劳动合同法第四十条"));

                when(
                                documentRetriever.retrieve(
                                                org.mockito.ArgumentMatchers.any(
                                                                Query.class)))
                                .thenReturn(retrievedDocuments);

                RetrieverContext result = operator.retrieve(context);

                ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(
                                Query.class);

                verify(documentRetriever).retrieve(
                                queryCaptor.capture());

                assertEquals(
                                "违法解除劳动合同是否合法",
                                queryCaptor.getValue().text());

                assertEquals(
                                2,
                                result.documentCount());

                assertEquals(
                                retrievedDocuments,
                                result.getDocuments());
        }

        @Test
        void shouldUseOriginalQuestionWhenRewriteDoesNotExist() {
                RetrieverContext context = RetrieverContext.from(
                                QueryContext.from(
                                                "劳动合同到期怎么办？"));

                when(
                                documentRetriever.retrieve(
                                                org.mockito.ArgumentMatchers.any(
                                                                Query.class)))
                                .thenReturn(List.of());

                operator.retrieve(context);

                ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(
                                Query.class);

                verify(documentRetriever).retrieve(
                                queryCaptor.capture());

                assertEquals(
                                "劳动合同到期怎么办？",
                                queryCaptor.getValue().text());
        }

        @Test
        void shouldReturnNewContextWithoutModifyingOriginal() {
                RetrieverContext originalContext = RetrieverContext.from(
                                QueryContext.from(
                                                "竞业协议合法吗？"));

                Document document = new Document(
                                "劳动合同法第二十三条");

                when(
                                documentRetriever.retrieve(
                                                org.mockito.ArgumentMatchers.any(
                                                                Query.class)))
                                .thenReturn(
                                                List.of(document));

                RetrieverContext result = operator.retrieve(
                                originalContext);

                assertNotSame(
                                originalContext,
                                result);

                assertFalse(
                                originalContext.hasDocuments());

                assertTrue(result.hasDocuments());
                assertEquals(1, result.documentCount());

                assertSame(
                                originalContext.getQueryContext(),
                                result.getQueryContext());
        }

        @Test
        void shouldPreserveOriginalQuestionAndRewriteQuery() {
                QueryContext queryContext = QueryContext.builder()
                                .question(
                                                "老板不给工资怎么办？")
                                .rewriteQuery(
                                                "拖欠劳动报酬的法律救济")
                                .build();

                RetrieverContext context = RetrieverContext.from(
                                queryContext);

                when(
                                documentRetriever.retrieve(
                                                org.mockito.ArgumentMatchers.any(
                                                                Query.class)))
                                .thenReturn(
                                                List.of(
                                                                new Document(
                                                                                "工资支付相关规定")));

                RetrieverContext result = operator.retrieve(context);

                assertEquals(
                                "老板不给工资怎么办？",
                                result.getQueryContext()
                                                .getQuestion());

                assertEquals(
                                "拖欠劳动报酬的法律救济",
                                result.getQueryContext()
                                                .getRewriteQuery());

                assertEquals(
                                "拖欠劳动报酬的法律救济",
                                result.effectiveQuery());
        }

        @Test
        void shouldReplaceExistingDocumentsWithVectorResults() {
                RetrieverContext context = RetrieverContext.builder()
                                .queryContext(
                                                QueryContext.from(
                                                                "劳动合同问题"))
                                .documents(
                                                List.of(
                                                                new Document(
                                                                                "旧文档")))
                                .build();

                Document newDocument = new Document(
                                "新的向量检索文档");

                when(
                                documentRetriever.retrieve(
                                                org.mockito.ArgumentMatchers.any(
                                                                Query.class)))
                                .thenReturn(
                                                List.of(newDocument));

                RetrieverContext result = operator.retrieve(context);

                assertEquals(
                                1,
                                result.documentCount());

                assertEquals(
                                "新的向量检索文档",
                                result.getDocuments()
                                                .get(0)
                                                .getText());
        }

        @Test
        void shouldNormalizeNullRetrieverResultToEmptyList() {
                RetrieverContext context = RetrieverContext.from(
                                QueryContext.from(
                                                "测试问题"));

                when(
                                documentRetriever.retrieve(
                                                org.mockito.ArgumentMatchers.any(
                                                                Query.class)))
                                .thenReturn(null);

                RetrieverContext result = operator.retrieve(context);

                assertFalse(result.hasDocuments());
                assertEquals(0, result.documentCount());
                assertTrue(result.getDocuments().isEmpty());
        }

        @Test
        void shouldReturnEmptyContextWhenNoDocumentsAreFound() {
                RetrieverContext context = RetrieverContext.from(
                                QueryContext.from(
                                                "没有匹配知识的问题"));

                when(
                                documentRetriever.retrieve(
                                                org.mockito.ArgumentMatchers.any(
                                                                Query.class)))
                                .thenReturn(List.of());

                RetrieverContext result = operator.retrieve(context);

                assertFalse(result.hasDocuments());
                assertEquals(0, result.documentCount());
        }

        @Test
        void shouldPropagateDocumentRetrieverException() {
                RetrieverContext context = RetrieverContext.from(
                                QueryContext.from(
                                                "测试问题"));

                IllegalStateException expectedException = new IllegalStateException(
                                "Vector store unavailable");

                when(
                                documentRetriever.retrieve(
                                                org.mockito.ArgumentMatchers.any(
                                                                Query.class)))
                                .thenThrow(expectedException);

                IllegalStateException actualException = assertThrows(
                                IllegalStateException.class,
                                () -> operator.retrieve(context));

                assertSame(
                                expectedException,
                                actualException);
        }

        @Test
        void shouldThrowExceptionWhenContextIsNull() {
                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> operator.retrieve(null));

                assertEquals(
                                "RetrieverContext must not be null",
                                exception.getMessage());

                verify(
                                documentRetriever,
                                never()).retrieve(
                                                org.mockito.ArgumentMatchers.any(
                                                                Query.class));
        }

        @Test
        void shouldRejectNullDocumentRetriever() {
                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> new VectorSearchOperator(
                                                null,
                                                tenantKnowledgeFilterFactory));

                assertEquals(
                                "documentRetriever must not be null",
                                exception.getMessage());
        }

        @Test
        void shouldApplySharedOnlyFilterWhenTenantIsMissing() {

                RetrieverContext context = RetrieverContext.from(
                                QueryContext.from(
                                                "劳动合同问题"));

                when(
                                documentRetriever.retrieve(
                                                org.mockito.ArgumentMatchers.any(
                                                                Query.class)))
                                .thenReturn(
                                                List.of());

                ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(
                                Query.class);

                operator.retrieve(
                                context);

                verify(
                                documentRetriever)
                                .retrieve(
                                                captor.capture());

                Query query = captor.getValue();

                assertEquals(
                                "knowledge_scope == 'SHARED'",
                                query.context()
                                                .get(
                                                                VectorStoreDocumentRetriever.FILTER_EXPRESSION));

                verify(
                                tenantKnowledgeFilterFactory)
                                .createSharedOnly();
        }

        @Test
        void shouldApplyTenantFilterWhenTenantExists() {

                RetrieverContext context = RetrieverContext.tenantAware(
                                QueryContext.from(
                                                "劳动合同问题"),
                                "tenant-a");

                when(
                                documentRetriever.retrieve(
                                                org.mockito.ArgumentMatchers.any(
                                                                Query.class)))
                                .thenReturn(
                                                List.of());

                ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(
                                Query.class);

                operator.retrieve(
                                context);

                verify(
                                documentRetriever)
                                .retrieve(
                                                captor.capture());

                Query query = captor.getValue();

                assertEquals(
                                "tenant-filter:tenant-a",
                                query.context()
                                                .get(
                                                                VectorStoreDocumentRetriever.FILTER_EXPRESSION));

                verify(
                                tenantKnowledgeFilterFactory)
                                .createForTenant(
                                                "tenant-a");
        }
}