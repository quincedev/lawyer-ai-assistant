package com.quince.lawyeraiassistant.retrieval.orchestration;

import com.quince.lawyeraiassistant.query.model.QueryContext;
import com.quince.lawyeraiassistant.query.pipeline.QueryPipeline;
import com.quince.lawyeraiassistant.retrieval.model.RetrieverContext;
import com.quince.lawyeraiassistant.retrieval.pipeline.RetrieverPipeline;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Query Pipeline 与 Retriever Pipeline 的检索编排器。
 *
 * <p>
 * 负责完成下面的调用流程：
 * </p>
 *
 * <pre>
 * question + conversationId
 *          ↓
 * QueryContext
 *          ↓
 * QueryPipeline
 *          ↓
 * RetrieverContext
 *          ↓
 * RetrieverPipeline
 *          ↓
 * 最终 RetrieverContext
 * </pre>
 *
 * <p>
 * 本类只负责编排，不实现具体的 Query 转换或检索算法。
 * </p>
 */
@Component
public class RetrievalOrchestrator {

        private final QueryPipeline queryPipeline;

        private final RetrieverPipeline retrieverPipeline;

        public RetrievalOrchestrator(
                        QueryPipeline queryPipeline,
                        RetrieverPipeline retrieverPipeline) {
                this.queryPipeline = Objects.requireNonNull(
                                queryPipeline,
                                "queryPipeline must not be null");

                this.retrieverPipeline = Objects.requireNonNull(
                                retrieverPipeline,
                                "retrieverPipeline must not be null");
        }

        /**
         * 执行不带会话编号的完整检索流程。
         *
         * @param question 用户原始问题
         * @return 检索流程最终上下文
         */
        public RetrieverContext retrieve(String question) {
                return retrieve(question, null);
        }

        /**
         * 执行完整的 Query + Retrieval 流程。
         *
         * @param question       用户原始问题
         * @param conversationId 可选会话编号
         * @return 检索流程最终上下文
         */
        public RetrieverContext retrieve(
                        String question,
                        String conversationId) {
                QueryContext initialQueryContext = QueryContext.from(
                                question,
                                conversationId);

                QueryContext transformedQueryContext = queryPipeline.execute(
                                initialQueryContext);

                if (transformedQueryContext == null) {
                        throw new IllegalStateException(
                                        "QueryPipeline must not return null");
                }

                RetrieverContext initialRetrieverContext = RetrieverContext.from(
                                transformedQueryContext);

                RetrieverContext retrievedContext = retrieverPipeline.retrieve(
                                initialRetrieverContext);

                if (retrievedContext == null) {
                        throw new IllegalStateException(
                                        "RetrieverPipeline must not return null");
                }

                return retrievedContext;
        }

        public RetrieverContext retrieveForTenant(
                        String question,
                        String tenantId) {

                return retrieveForTenant(
                                question,
                                null,
                                tenantId);
        }

        public RetrieverContext retrieveForTenant(
                        String question,
                        String conversationId,
                        String tenantId) {

                Objects.requireNonNull(
                                tenantId,
                                "tenantId must not be null");

                String normalizedTenantId = tenantId.trim();

                if (normalizedTenantId.isEmpty()) {

                        throw new IllegalArgumentException(
                                        "tenantId must not be blank");
                }

                QueryContext initialQueryContext = QueryContext.from(
                                question,
                                conversationId);

                QueryContext transformedQueryContext = queryPipeline.execute(
                                initialQueryContext);

                if (transformedQueryContext == null) {

                        throw new IllegalStateException(
                                        "QueryPipeline must not return null");
                }

                RetrieverContext initialRetrieverContext = RetrieverContext.tenantAware(
                                transformedQueryContext,
                                normalizedTenantId);

                RetrieverContext retrievedContext = retrieverPipeline.retrieve(
                                initialRetrieverContext);

                if (retrievedContext == null) {

                        throw new IllegalStateException(
                                        "RetrieverPipeline must not return null");
                }

                return retrievedContext;
        }
}