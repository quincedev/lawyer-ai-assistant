package com.quince.lawyeraiassistant.rag.vector.sync;

import java.util.Objects;

import com.quince.lawyeraiassistant.cache.retrieval.RetrievalCache;
import com.quince.lawyeraiassistant.cache.tool.ToolResultCache;
import com.quince.lawyeraiassistant.rag.config.ConditionalOnRagEnabled;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnRagEnabled
public class DefaultKnowledgeCacheInvalidator
        implements KnowledgeCacheInvalidator {

    private final RetrievalCache retrievalCache;

    private final ToolResultCache toolResultCache;

    public DefaultKnowledgeCacheInvalidator(
            RetrievalCache retrievalCache,
            ToolResultCache toolResultCache) {

        this.retrievalCache = Objects.requireNonNull(
                retrievalCache,
                "retrievalCache must not be null");

        this.toolResultCache = Objects.requireNonNull(
                toolResultCache,
                "toolResultCache must not be null");
    }

    @Override
    public void invalidateAfter(
            KnowledgeSyncResult result) {

        Objects.requireNonNull(
                result,
                "result must not be null");

        if (!result.hasKnowledgeChanges()) {

            log.info(
                    "Knowledge cache invalidation skipped. "
                            + "reason=UNCHANGED");

            return;
        }

        /*
         * Retrieval Cache 保存最终检索结果；
         * Tool Cache 保存 MCP Legal Tool 的最终结果。
         *
         * 知识内容变化后两者都可能过期。
         */
        retrievalCache.clear();

        toolResultCache.clear();

        log.info(
                "Knowledge caches invalidated. "
                        + "addedSources={}, replacedSources={}, "
                        + "removedSources={}",
                result.addedSources(),
                result.replacedSources(),
                result.removedSources());
    }
}
