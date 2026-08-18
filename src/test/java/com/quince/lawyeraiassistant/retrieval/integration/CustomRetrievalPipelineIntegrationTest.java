package com.quince.lawyeraiassistant.retrieval.integration;

import com.quince.lawyeraiassistant.query.pipeline.DefaultQueryPipeline;
import com.quince.lawyeraiassistant.query.pipeline.QueryPipeline;
import com.quince.lawyeraiassistant.query.transformer.RewriteTransformer;
import com.quince.lawyeraiassistant.rag.vector.tenant.TenantKnowledgeFilterFactory;
import com.quince.lawyeraiassistant.retrieval.model.RetrieverContext;
import com.quince.lawyeraiassistant.retrieval.operator.VectorSearchOperator;
import com.quince.lawyeraiassistant.retrieval.orchestration.RetrievalOrchestrator;
import com.quince.lawyeraiassistant.retrieval.pipeline.DefaultRetrieverPipeline;
import com.quince.lawyeraiassistant.retrieval.pipeline.RetrieverPipeline;
import com.quince.lawyeraiassistant.security.SecurityTest;

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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Query Pipeline 与 Retriever Pipeline 的组件集成测试。
 *
 * <p>
 * 本测试使用真实 Pipeline 和 Operator，
 * 仅 Mock 最底层 DocumentRetriever。
 * </p>
 */
class CustomRetrievalPipelineIntegrationTest {

        private DocumentRetriever documentRetriever;

        private RetrievalOrchestrator retrievalOrchestrator;

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
                                                                anyString()))
                                .thenAnswer(
                                                invocation -> "tenant-filter:"
                                                                + invocation.getArgument(
                                                                                0));

                QueryPipeline queryPipeline = new DefaultQueryPipeline(
                                List.of(
                                                new RewriteTransformer()));

                VectorSearchOperator vectorSearchOperator = new VectorSearchOperator(
                                documentRetriever,
                                tenantKnowledgeFilterFactory);

                RetrieverPipeline retrieverPipeline = new DefaultRetrieverPipeline(
                                List.of(
                                                vectorSearchOperator));

                retrievalOrchestrator = new RetrievalOrchestrator(
                                queryPipeline,
                                retrieverPipeline);
        }

        @Test
        void shouldExecuteCompleteQueryAndRetrievalPipeline() {
                List<Document> retrievedDocuments = List.of(
                                new Document(
                                                "《劳动合同法》第三十九条规定了"
                                                                + "用人单位可以解除劳动合同的情形。"),
                                new Document(
                                                "《劳动合同法》第四十六条规定了"
                                                                + "经济补偿的适用情形。"));

                when(
                                documentRetriever.retrieve(
                                                any(Query.class)))
                                .thenReturn(retrievedDocuments);

                RetrieverContext result = retrievalOrchestrator.retrieve(
                                "老板把我开了合法吗？",
                                "conversation-001");

                assertEquals(
                                "老板把我开了合法吗？",
                                result.getQueryContext()
                                                .getQuestion());

                /*
                 * 当前 RewriteTransformer 仍是 Dummy，
                 * 所以 rewriteQuery 与原始问题相同。
                 */
                assertEquals(
                                "老板把我开了合法吗？",
                                result.getQueryContext()
                                                .getRewriteQuery());

                assertEquals(
                                "老板把我开了合法吗？",
                                result.effectiveQuery());

                assertEquals(
                                "conversation-001",
                                result.getQueryContext()
                                                .getConversationId());

                assertTrue(result.hasDocuments());
                assertEquals(2, result.documentCount());

                assertEquals(
                                retrievedDocuments,
                                result.getDocuments());
        }

        @Test
        void shouldPassEffectiveQueryToDocumentRetriever() {
                when(
                                documentRetriever.retrieve(
                                                any(Query.class)))
                                .thenReturn(List.of());

                retrievalOrchestrator.retrieve(
                                "劳动合同解除需要赔偿吗？");

                ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(
                                Query.class);

                verify(documentRetriever).retrieve(
                                queryCaptor.capture());

                assertEquals(
                                "劳动合同解除需要赔偿吗？",
                                queryCaptor.getValue().text());
        }

        @Test
        void shouldInvokeDocumentRetrieverOnlyOncePerPipelineExecution() {
                when(
                                documentRetriever.retrieve(
                                                any(Query.class)))
                                .thenReturn(
                                                List.of(
                                                                new Document(
                                                                                "劳动合同解除相关规定")));

                retrievalOrchestrator.retrieve(
                                "劳动合同解除是否合法？");

                verify(
                                documentRetriever,
                                times(1)).retrieve(any(Query.class));
        }

        @Test
        void shouldReturnEmptyDocumentsWhenNothingIsRetrieved() {
                when(
                                documentRetriever.retrieve(
                                                any(Query.class)))
                                .thenReturn(List.of());

                RetrieverContext result = retrievalOrchestrator.retrieve(
                                "知识库中不存在的问题");

                assertFalse(result.hasDocuments());
                assertEquals(0, result.documentCount());

                /*
                 * 没有检索结果时，QueryContext 仍必须保留。
                 */
                assertEquals(
                                "知识库中不存在的问题",
                                result.getQueryContext()
                                                .getQuestion());
        }

        @Test
        void shouldKeepOriginalQuestionSeparateFromRetrievalQuery() {
                /*
                 * 本测试单独构造真实 QueryPipeline，
                 * 使用一个模拟“真实改写”的 Transformer。
                 */
                QueryPipeline queryPipeline = new DefaultQueryPipeline(
                                List.of(
                                                context -> context.toBuilder()
                                                                .rewriteQuery(
                                                                                "违法解除劳动合同")
                                                                .build()));

                RetrieverPipeline retrieverPipeline = new DefaultRetrieverPipeline(
                                List.of(
                                                new VectorSearchOperator(
                                                                documentRetriever,
                                                                tenantKnowledgeFilterFactory)));

                RetrievalOrchestrator orchestrator = new RetrievalOrchestrator(
                                queryPipeline,
                                retrieverPipeline);

                when(
                                documentRetriever.retrieve(
                                                any(Query.class)))
                                .thenReturn(
                                                List.of(
                                                                new Document(
                                                                                "违法解除劳动合同相关规定")));

                RetrieverContext result = orchestrator.retrieve(
                                "老板突然不要我了，合法吗？");

                ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(
                                Query.class);

                verify(documentRetriever).retrieve(
                                queryCaptor.capture());

                /*
                 * Retriever 使用改写后的 Query。
                 */
                assertEquals(
                                "违法解除劳动合同",
                                queryCaptor.getValue().text());

                /*
                 * 原始问题仍然保留，后续可交给 Prompt Pipeline。
                 */
                assertEquals(
                                "老板突然不要我了，合法吗？",
                                result.getQueryContext()
                                                .getQuestion());

                assertEquals(
                                "违法解除劳动合同",
                                result.effectiveQuery());
        }

        @SecurityTest
        @Test
        void shouldApplySharedOnlyFilterForLegacyRetrieval() {

                when(
                                documentRetriever.retrieve(
                                                any(
                                                                Query.class)))
                                .thenReturn(
                                                List.of());

                retrievalOrchestrator.retrieve(
                                "劳动合同解除需要赔偿吗？");

                ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(
                                Query.class);

                verify(
                                documentRetriever)
                                .retrieve(
                                                queryCaptor.capture());

                Query query = queryCaptor.getValue();

                assertEquals(
                                "knowledge_scope == 'SHARED'",
                                query.context()
                                                .get(
                                                                VectorStoreDocumentRetriever.FILTER_EXPRESSION));

                verify(
                                tenantKnowledgeFilterFactory)
                                .createSharedOnly();
        }

        @SecurityTest
        @Test
        void shouldPropagateTenantFilterThroughCompleteRetrievalPipeline() {

                when(
                                documentRetriever.retrieve(
                                                any(
                                                                Query.class)))
                                .thenReturn(
                                                List.of());

                RetrieverContext result = retrievalOrchestrator.retrieveForTenant(
                                "劳动合同解除需要赔偿吗？",
                                "tenant-a");

                ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(
                                Query.class);

                verify(
                                documentRetriever)
                                .retrieve(
                                                queryCaptor.capture());

                Query query = queryCaptor.getValue();

                assertEquals(
                                "tenant-filter:tenant-a",
                                query.context()
                                                .get(
                                                                VectorStoreDocumentRetriever.FILTER_EXPRESSION));

                assertTrue(
                                result.hasTenantId());

                assertEquals(
                                "tenant-a",
                                result.requireTenantId());

                verify(
                                tenantKnowledgeFilterFactory)
                                .createForTenant(
                                                "tenant-a");
        }
}