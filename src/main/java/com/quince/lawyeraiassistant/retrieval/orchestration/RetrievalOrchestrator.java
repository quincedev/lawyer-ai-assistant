package com.quince.lawyeraiassistant.retrieval.orchestration;

import com.quince.lawyeraiassistant.cache.CacheKeyFactory;
import com.quince.lawyeraiassistant.cache.CacheScope;
import com.quince.lawyeraiassistant.cache.config.AiCacheProperties;
import com.quince.lawyeraiassistant.cache.retrieval.CachedRetrievalResult;
import com.quince.lawyeraiassistant.cache.retrieval.RetrievalCache;
import com.quince.lawyeraiassistant.performance.PerformanceTimer;
import com.quince.lawyeraiassistant.query.model.QueryContext;
import com.quince.lawyeraiassistant.query.pipeline.QueryPipeline;
import com.quince.lawyeraiassistant.rag.config.RetrievalProperties;
import com.quince.lawyeraiassistant.retrieval.model.RetrieverContext;
import com.quince.lawyeraiassistant.retrieval.pipeline.RetrieverPipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Query Pipeline 与 Retriever Pipeline 的检索编排器。
 *
 * <p>
 * Step 5 在原有 Query + Retrieval 编排基础上增加 Cache-Aside：
 * </p>
 *
 * <pre>
 * question
 *    ↓
 * QueryPipeline
 *    ↓
 * effectiveQuery
 *    ↓
 * RetrievalCache
 *    ├─ HIT  → 当前请求 Context + Cached Documents
 *    └─ MISS → RetrieverPipeline → Cache PUT
 * </pre>
 *
 * <p>
 * 注意：缓存只保存 Documents，不保存完整 RetrieverContext，
 * 避免把上一请求的 QueryContext / tenant runtime state 带入当前请求。
 * </p>
 */
@Component
public class RetrievalOrchestrator {

        private static final Logger log = LoggerFactory.getLogger(
                        RetrievalOrchestrator.class);

        private final QueryPipeline queryPipeline;

        private final RetrieverPipeline retrieverPipeline;

        private final RetrievalCache retrievalCache;

        private final CacheKeyFactory cacheKeyFactory;

        private final AiCacheProperties cacheProperties;

        private final RetrievalProperties retrievalProperties;

        public RetrievalOrchestrator(
                        QueryPipeline queryPipeline,
                        RetrieverPipeline retrieverPipeline,
                        RetrievalCache retrievalCache,
                        CacheKeyFactory cacheKeyFactory,
                        AiCacheProperties cacheProperties,
                        RetrievalProperties retrievalProperties) {

                this.queryPipeline = Objects.requireNonNull(
                                queryPipeline,
                                "queryPipeline must not be null");

                this.retrieverPipeline = Objects.requireNonNull(
                                retrieverPipeline,
                                "retrieverPipeline must not be null");

                this.retrievalCache = Objects.requireNonNull(
                                retrievalCache,
                                "retrievalCache must not be null");

                this.cacheKeyFactory = Objects.requireNonNull(
                                cacheKeyFactory,
                                "cacheKeyFactory must not be null");

                this.cacheProperties = Objects.requireNonNull(
                                cacheProperties,
                                "cacheProperties must not be null");

                this.retrievalProperties = Objects.requireNonNull(
                                retrievalProperties,
                                "retrievalProperties must not be null");
        }

        /**
         * 执行不带会话编号的 SHARED-only 检索。
         */
        public RetrieverContext retrieve(
                        String question) {

                return retrieve(
                                question,
                                null);
        }

        /**
         * 执行 SHARED-only Query + Retrieval 流程。
         */
        public RetrieverContext retrieve(
                        String question,
                        String conversationId) {

                PerformanceTimer timer = PerformanceTimer.start();

                try {

                        QueryContext transformedQueryContext = executeQueryPipeline(
                                        question,
                                        conversationId);

                        RetrieverContext initialRetrieverContext = RetrieverContext.from(
                                        transformedQueryContext);

                        if (!isRetrievalCacheEnabled()) {

                                return executeRetrieval(
                                                initialRetrieverContext);
                        }

                        String cacheKey = cacheKeyFactory.retrievalKey(
                                        CacheScope.SHARED,
                                        null,
                                        transformedQueryContext.effectiveQuery(),
                                        cacheProperties.getKnowledgeVersion(),
                                        retrievalProperties.topK(),
                                        retrievalProperties.similarityThreshold());

                        return retrievalCache.get(
                                        cacheKey)
                                        .map(
                                                        cached -> {

                                                                log.info(
                                                                                "RAG retrieval cache hit. scope=SHARED, documentCount={}",
                                                                                cached.documentCount());

                                                                return restoreCachedResult(
                                                                                initialRetrieverContext,
                                                                                cached);
                                                        })
                                        .orElseGet(
                                                        () -> {

                                                                log.info(
                                                                                "RAG retrieval cache miss. scope=SHARED");

                                                                RetrieverContext result = executeRetrieval(
                                                                                initialRetrieverContext);

                                                                cacheSuccessfulRetrieval(
                                                                                cacheKey,
                                                                                result);

                                                                return result;
                                                        });

                } finally {

                        log.info(
                                        "RAG retrieval finished. tenantAware=false, durationMs={}",
                                        timer.elapsedMillis());
                }
        }

        /**
         * Tenant-aware 检索便捷入口。
         */
        public RetrieverContext retrieveForTenant(
                        String question,
                        String tenantId) {

                return retrieveForTenant(
                                question,
                                null,
                                tenantId);
        }

        /**
         * Tenant-aware Query + Retrieval 流程。
         *
         * <p>
         * Cache Key 强制包含可信 tenantId，避免 Tenant A / Tenant B 串缓存。
         * </p>
         */
        public RetrieverContext retrieveForTenant(
                        String question,
                        String conversationId,
                        String tenantId) {

                PerformanceTimer timer = PerformanceTimer.start();

                try {

                        String normalizedTenantId = normalizeTenantId(
                                        tenantId);

                        QueryContext transformedQueryContext = executeQueryPipeline(
                                        question,
                                        conversationId);

                        RetrieverContext initialRetrieverContext = RetrieverContext.tenantAware(
                                        transformedQueryContext,
                                        normalizedTenantId);

                        if (!isRetrievalCacheEnabled()) {

                                return executeRetrieval(
                                                initialRetrieverContext);
                        }

                        String cacheKey = cacheKeyFactory.retrievalKey(
                                        CacheScope.TENANT,
                                        normalizedTenantId,
                                        transformedQueryContext.effectiveQuery(),
                                        cacheProperties.getKnowledgeVersion(),
                                        retrievalProperties.topK(),
                                        retrievalProperties.similarityThreshold());

                        return retrievalCache.get(
                                        cacheKey)
                                        .map(
                                                        cached -> {

                                                                log.info(
                                                                                "RAG retrieval cache hit. scope=TENANT, tenantId={}, documentCount={}",
                                                                                normalizedTenantId,
                                                                                cached.documentCount());

                                                                return restoreCachedResult(
                                                                                initialRetrieverContext,
                                                                                cached);
                                                        })
                                        .orElseGet(
                                                        () -> {

                                                                log.info(
                                                                                "RAG retrieval cache miss. scope=TENANT, tenantId={}",
                                                                                normalizedTenantId);

                                                                RetrieverContext result = executeRetrieval(
                                                                                initialRetrieverContext);

                                                                cacheSuccessfulRetrieval(
                                                                                cacheKey,
                                                                                result);

                                                                return result;
                                                        });

                } finally {

                        log.info(
                                        "RAG retrieval finished. tenantAware=true, durationMs={}",
                                        timer.elapsedMillis());
                }
        }

        private QueryContext executeQueryPipeline(
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

                return transformedQueryContext;
        }

        private RetrieverContext executeRetrieval(
                        RetrieverContext initialContext) {

                RetrieverContext retrievedContext = retrieverPipeline.retrieve(
                                initialContext);

                if (retrievedContext == null) {

                        throw new IllegalStateException(
                                        "RetrieverPipeline must not return null");
                }

                return retrievedContext;
        }

        private RetrieverContext restoreCachedResult(
                        RetrieverContext currentContext,
                        CachedRetrievalResult cached) {

                Objects.requireNonNull(
                                currentContext,
                                "currentContext must not be null");

                Objects.requireNonNull(
                                cached,
                                "cached must not be null");

                return currentContext.toBuilder()
                                .documents(
                                                cached.documents())
                                .build();
        }

        /**
         * 第一版只缓存非空成功结果。
         *
         * <p>
         * 空结果不做 negative cache，避免暂时性无结果被长期复用。
         * </p>
         */
        private void cacheSuccessfulRetrieval(
                        String cacheKey,
                        RetrieverContext result) {

                if (result == null
                                || !result.hasDocuments()) {

                        return;
                }

                retrievalCache.put(
                                cacheKey,
                                new CachedRetrievalResult(
                                                result.getDocuments()));

                log.info(
                                "RAG retrieval cached. documentCount={}",
                                result.documentCount());
        }

        private boolean isRetrievalCacheEnabled() {

                return cacheProperties.isEnabled()
                                && cacheProperties
                                                .getRetrieval()
                                                .isEnabled();
        }

        private String normalizeTenantId(
                        String tenantId) {

                Objects.requireNonNull(
                                tenantId,
                                "tenantId must not be null");

                String normalized = tenantId.trim();

                if (normalized.isEmpty()) {

                        throw new IllegalArgumentException(
                                        "tenantId must not be blank");
                }

                return normalized;
        }
}