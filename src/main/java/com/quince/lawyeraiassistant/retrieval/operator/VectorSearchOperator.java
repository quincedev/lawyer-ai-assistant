package com.quince.lawyeraiassistant.retrieval.operator;

import com.quince.lawyeraiassistant.rag.vector.tenant.TenantKnowledgeFilterFactory;
import com.quince.lawyeraiassistant.retrieval.model.RetrieverContext;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 基于 Spring AI DocumentRetriever 的向量检索节点。
 *
 * <p>
 * 执行流程：
 * </p>
 *
 * <pre>
 * RetrieverContext
 *      ↓
 * effectiveQuery()
 *      ↓
 * Spring AI Query
 *      ↓
 * DocumentRetriever
 *      ↓
 * List&lt;Document&gt;
 *      ↓
 * 新的 RetrieverContext
 * </pre>
 *
 * <p>
 * 本 Operator 不直接依赖 VectorStore，也不负责配置 topK、
 * similarityThreshold 或 metadata filter。
 * 这些检索参数由 DocumentRetriever Bean 统一管理。
 * </p>
 */
@Component
@Order(100)
public class VectorSearchOperator
                implements RetrievalOperator {

        private final DocumentRetriever documentRetriever;

        private final TenantKnowledgeFilterFactory tenantKnowledgeFilterFactory;

        public VectorSearchOperator(
                        DocumentRetriever documentRetriever,
                        TenantKnowledgeFilterFactory tenantKnowledgeFilterFactory) {
                this.documentRetriever = Objects.requireNonNull(
                                documentRetriever,
                                "documentRetriever must not be null");

                this.tenantKnowledgeFilterFactory = Objects.requireNonNull(
                                tenantKnowledgeFilterFactory,
                                "tenantKnowledgeFilterFactory must not be null");
        }

        /**
         * 使用当前有效 Query 执行向量检索。
         *
         * @param context 当前检索上下文
         * @return 包含向量检索文档的新 RetrieverContext
         */
        @Override
        public RetrieverContext retrieve(
                        RetrieverContext context) {
                Objects.requireNonNull(
                                context,
                                "RetrieverContext must not be null");

                String filterExpression = context.hasTenantId()
                                ? tenantKnowledgeFilterFactory.createForTenant(
                                                context.requireTenantId())
                                : tenantKnowledgeFilterFactory.createSharedOnly();

                Query query = Query.builder()
                                .text(
                                                context.effectiveQuery())
                                .context(
                                                Map.of(
                                                                VectorStoreDocumentRetriever.FILTER_EXPRESSION,
                                                                filterExpression))
                                .build();

                List<Document> documents = documentRetriever.retrieve(query);

                return context.toBuilder()
                                .documents(
                                                normalizeDocuments(documents))
                                .build();
        }

        /**
         * 将异常的 null 返回值统一规范为空集合。
         *
         * <p>
         * Spring AI 的标准实现通常返回文档列表，但作为调用方，
         * 当前组件仍对自定义 DocumentRetriever 的 null 返回值
         * 做防御性处理。
         * </p>
         */
        private List<Document> normalizeDocuments(
                        List<Document> documents) {
                return documents == null
                                ? List.of()
                                : documents;
        }
}