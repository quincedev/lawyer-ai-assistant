package com.quince.lawyeraiassistant.agent.tool.legal.cache;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.quince.lawyeraiassistant.agent.model.ToolAction;
import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceContext;
import com.quince.lawyeraiassistant.agent.tool.ToolExecutionContext;
import com.quince.lawyeraiassistant.cache.CacheKeyFactory;
import com.quince.lawyeraiassistant.cache.CacheScope;
import com.quince.lawyeraiassistant.cache.config.AiCacheProperties;
import com.quince.lawyeraiassistant.cache.tool.ToolCachePolicy;
import com.quince.lawyeraiassistant.cache.tool.ToolResultCache;
import com.quince.lawyeraiassistant.knowledge.contract.LegalToolContract;

@Component
public final class DefaultLegalToolCache
        implements LegalToolCache {

    private static final Logger log = LoggerFactory.getLogger(
            DefaultLegalToolCache.class);

    private static final String TOOL_NAME = LegalToolContract.SEARCH_LEGAL_KNOWLEDGE;

    private final ToolResultCache toolResultCache;

    private final ToolCachePolicy toolCachePolicy;

    private final CacheKeyFactory cacheKeyFactory;

    private final AiCacheProperties cacheProperties;

    private final AgentPerformanceContext performanceContext;

    public DefaultLegalToolCache(
            ToolResultCache toolResultCache,
            ToolCachePolicy toolCachePolicy,
            CacheKeyFactory cacheKeyFactory,
            AiCacheProperties cacheProperties,
            AgentPerformanceContext performanceContext) {

        this.toolResultCache = Objects.requireNonNull(
                toolResultCache,
                "toolResultCache must not be null");

        this.toolCachePolicy = Objects.requireNonNull(
                toolCachePolicy,
                "toolCachePolicy must not be null");

        this.cacheKeyFactory = Objects.requireNonNull(
                cacheKeyFactory,
                "cacheKeyFactory must not be null");

        this.cacheProperties = Objects.requireNonNull(
                cacheProperties,
                "cacheProperties must not be null");

        this.performanceContext = Objects.requireNonNull(
                performanceContext,
                "performanceContext must not be null");
    }

    @Override
    public Optional<LegalToolCacheHit> get(
            ToolExecutionContext executionContext,
            ToolAction action) {

        Objects.requireNonNull(
                executionContext,
                "ToolExecutionContext must not be null");

        Objects.requireNonNull(
                action,
                "ToolAction must not be null");

        String cacheKey = resolveToolCacheKey(
                executionContext,
                action);

        if (cacheKey == null) {
            return Optional.empty();
        }

        Optional<String> cached = toolResultCache.get(
                cacheKey);

        if (cached.isPresent()) {

            performanceContext
                    .current()
                    .ifPresent(
                            metrics -> metrics.recordCacheHit());

            log.info(
                    "Tool result cache hit. toolName={}, scope={}",
                    TOOL_NAME,
                    resolveCacheScope(
                            executionContext));

            return cached.map(
                    LegalToolCacheHit::new);
        }

        performanceContext
                .current()
                .ifPresent(
                        metrics -> metrics.recordCacheMiss());

        log.info(
                "Tool result cache miss. toolName={}, scope={}",
                TOOL_NAME,
                resolveCacheScope(
                        executionContext));

        return Optional.empty();
    }

    @Override
    public void put(
            ToolExecutionContext executionContext,
            ToolAction action,
            String rawResult) {

        Objects.requireNonNull(
                executionContext,
                "ToolExecutionContext must not be null");

        Objects.requireNonNull(
                action,
                "ToolAction must not be null");

        if (rawResult == null
                || rawResult.isBlank()) {
            return;
        }

        String cacheKey = resolveToolCacheKey(
                executionContext,
                action);

        if (cacheKey == null) {
            return;
        }

        toolResultCache.put(
                cacheKey,
                rawResult);

        log.info(
                "Tool result cached. toolName={}, scope={}",
                TOOL_NAME,
                resolveCacheScope(
                        executionContext));
    }

    private String resolveToolCacheKey(
            ToolExecutionContext executionContext,
            ToolAction action) {

        if (!isToolCacheEnabled()
                || !toolCachePolicy.isCacheable(
                        action.getToolName())) {

            return null;
        }

        CacheScope scope = resolveCacheScope(
                executionContext);

        String tenantId = scope == CacheScope.TENANT
                ? executionContext
                        .requireTenantContext()
                        .tenantId()
                : null;

        Map<String, Object> cacheArguments = resolveToolCacheArguments(
                executionContext,
                action);

        return cacheKeyFactory.toolKey(
                scope,
                tenantId,
                action.getToolName(),
                cacheArguments,
                cacheProperties.getKnowledgeVersion());
    }

    private CacheScope resolveCacheScope(
            ToolExecutionContext executionContext) {

        return executionContext.hasTenantContext()
                ? CacheScope.TENANT
                : CacheScope.SHARED;
    }

    private boolean isToolCacheEnabled() {

        return cacheProperties.isEnabled()
                && cacheProperties
                        .getTool()
                        .isEnabled();
    }

    private Map<String, Object> resolveToolCacheArguments(
            ToolExecutionContext executionContext,
            ToolAction action) {

        if (TOOL_NAME.equals(
                action.getToolName())
                && executionContext.hasExecutionGoal()) {

            return Map.of(
                    "goal",
                    executionContext.requireExecutionGoal());
        }

        Map<String, Object> originalArguments = action.getArguments();

        return originalArguments == null
                ? Map.of()
                : originalArguments;
    }
}