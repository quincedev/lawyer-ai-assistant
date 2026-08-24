package com.quince.lawyeraiassistant.agent.tool.legal.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.ToolAction;
import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceContext;
import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceMetrics;
import com.quince.lawyeraiassistant.agent.tool.ToolExecutionContext;
import com.quince.lawyeraiassistant.agent.tool.legal.LegalKnowledgeTool;
import com.quince.lawyeraiassistant.cache.CacheKeyFactory;
import com.quince.lawyeraiassistant.cache.CacheScope;
import com.quince.lawyeraiassistant.cache.config.AiCacheProperties;
import com.quince.lawyeraiassistant.cache.tool.ToolCachePolicy;
import com.quince.lawyeraiassistant.cache.tool.ToolResultCache;
import com.quince.lawyeraiassistant.security.identity.UserRole;
import com.quince.lawyeraiassistant.security.tenant.TenantContext;

import tools.jackson.databind.ObjectMapper;

import static org.mockito.Mockito.mock;

class DefaultLegalToolCacheTest {

    private ToolResultCache toolResultCache;

    private ToolCachePolicy toolCachePolicy;

    private CacheKeyFactory cacheKeyFactory;

    private AiCacheProperties cacheProperties;

    private AgentPerformanceContext performanceContext;

    private DefaultLegalToolCache cache;

    @BeforeEach
    void setUp() {

        toolResultCache = mock(ToolResultCache.class);

        toolCachePolicy = new ToolCachePolicy();

        cacheKeyFactory = new CacheKeyFactory(
                new ObjectMapper());

        cacheProperties = new AiCacheProperties();

        cacheProperties.setEnabled(
                true);

        cacheProperties.getTool()
                .setEnabled(
                        true);

        cacheProperties.setKnowledgeVersion(
                "v1");

        performanceContext = new AgentPerformanceContext();

        cache = new DefaultLegalToolCache(
                toolResultCache,
                toolCachePolicy,
                cacheKeyFactory,
                cacheProperties,
                performanceContext);
    }

    @Test
    void disabledGlobalCacheShouldNotAccessUnderlyingCache() {

        cacheProperties.setEnabled(
                false);

        Optional<LegalToolCacheHit> result = cache.get(
                ToolExecutionContext.sharedOnly(),
                action(
                        "question"));

        assertTrue(
                result.isEmpty());

        verify(
                toolResultCache,
                never())
                .get(
                        anyString());
    }

    @Test
    void disabledToolCacheShouldNotAccessUnderlyingCache() {

        cacheProperties.getTool()
                .setEnabled(
                        false);

        Optional<LegalToolCacheHit> result = cache.get(
                ToolExecutionContext.sharedOnly(),
                action(
                        "question"));

        assertTrue(
                result.isEmpty());

        verify(
                toolResultCache,
                never())
                .get(
                        anyString());
    }

    @Test
    void nonCacheableToolShouldNotAccessUnderlyingCache() {

        ToolAction action = ToolAction.of(
                "task",
                "anotherTool",
                Map.of(
                        "question",
                        "test"));

        Optional<LegalToolCacheHit> result = cache.get(
                ToolExecutionContext.sharedOnly(),
                action);

        assertTrue(
                result.isEmpty());

        verify(
                toolResultCache,
                never())
                .get(
                        anyString());
    }

    @Test
    void sharedCacheHitShouldReturnRawResultAndRecordHit() {

        ToolExecutionContext context = ToolExecutionContext.sharedOnly();

        ToolAction action = action(
                "question");

        String expectedKey = cacheKeyFactory.toolKey(
                CacheScope.SHARED,
                null,
                action.getToolName(),
                action.getArguments(),
                "v1");

        when(toolResultCache.get(
                expectedKey))
                .thenReturn(
                        Optional.of(
                                "raw-result"));

        AgentPerformanceMetrics metrics = performanceContext.start();

        try {

            Optional<LegalToolCacheHit> result = cache.get(
                    context,
                    action);

            assertTrue(
                    result.isPresent());

            assertEquals(
                    "raw-result",
                    result.orElseThrow()
                            .rawResult());

            assertEquals(
                    1,
                    metrics.snapshot()
                            .cacheHits());

            assertEquals(
                    0,
                    metrics.snapshot()
                            .cacheMisses());

        } finally {

            performanceContext.clear();
        }
    }

    @Test
    void sharedCacheMissShouldRecordMiss() {

        ToolExecutionContext context = ToolExecutionContext.sharedOnly();

        ToolAction action = action(
                "question");

        when(toolResultCache.get(
                anyString()))
                .thenReturn(
                        Optional.empty());

        AgentPerformanceMetrics metrics = performanceContext.start();

        try {

            Optional<LegalToolCacheHit> result = cache.get(
                    context,
                    action);

            assertTrue(
                    result.isEmpty());

            assertEquals(
                    0,
                    metrics.snapshot()
                            .cacheHits());

            assertEquals(
                    1,
                    metrics.snapshot()
                            .cacheMisses());

        } finally {

            performanceContext.clear();
        }
    }

    @Test
    void tenantIdShouldParticipateInCacheKey() {

        ToolExecutionContext tenantA = tenantContext(
                "tenant-a",
                "same-goal");

        ToolExecutionContext tenantB = tenantContext(
                "tenant-b",
                "same-goal");

        ToolAction action = action(
                "same-question");

        cache.get(
                tenantA,
                action);

        cache.get(
                tenantB,
                action);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(
                String.class);

        verify(
                toolResultCache,
                org.mockito.Mockito.times(2))
                .get(
                        keyCaptor.capture());

        assertEquals(
                2,
                keyCaptor.getAllValues()
                        .stream()
                        .distinct()
                        .count());
    }

    @Test
    void sharedAndTenantScopeShouldUseDifferentCacheKeys() {

        ToolExecutionContext shared = ToolExecutionContext.sharedOnly();

        ToolExecutionContext tenant = tenantContext(
                "tenant-a",
                null);

        ToolAction action = action(
                "same-question");

        cache.get(
                shared,
                action);

        cache.get(
                tenant,
                action);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(
                String.class);

        verify(
                toolResultCache,
                org.mockito.Mockito.times(2))
                .get(
                        keyCaptor.capture());

        String sharedKey = keyCaptor.getAllValues()
                .get(0);

        String tenantKey = keyCaptor.getAllValues()
                .get(1);

        assertFalse(
                sharedKey.equals(
                        tenantKey));
    }

    @Test
    void sameExecutionGoalShouldProduceStableKeyDespiteDifferentLegalQuestions() {

        ToolExecutionContext context = tenantContext(
                "tenant-a",
                "分析违法解除劳动合同责任");

        ToolAction firstAction = action(
                "违法解除劳动合同的法律责任是什么？");

        ToolAction secondAction = action(
                "请说明违法解除时用人单位承担什么赔偿责任");

        cache.get(
                context,
                firstAction);

        cache.get(
                context,
                secondAction);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(
                String.class);

        verify(
                toolResultCache,
                org.mockito.Mockito.times(2))
                .get(
                        keyCaptor.capture());

        assertEquals(
                keyCaptor.getAllValues()
                        .get(0),
                keyCaptor.getAllValues()
                        .get(1));
    }

    @Test
    void withoutExecutionGoalOriginalArgumentsShouldAffectKey() {

        ToolExecutionContext context = ToolExecutionContext.sharedOnly();

        ToolAction firstAction = action(
                "question-a");

        ToolAction secondAction = action(
                "question-b");

        cache.get(
                context,
                firstAction);

        cache.get(
                context,
                secondAction);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(
                String.class);

        verify(
                toolResultCache,
                org.mockito.Mockito.times(2))
                .get(
                        keyCaptor.capture());

        assertFalse(
                keyCaptor.getAllValues()
                        .get(0)
                        .equals(
                                keyCaptor.getAllValues()
                                        .get(1)));
    }

    @Test
    void knowledgeVersionShouldParticipateInCacheKey() {

        ToolExecutionContext context = ToolExecutionContext.sharedOnly();

        ToolAction action = action(
                "question");

        cache.get(
                context,
                action);

        cacheProperties.setKnowledgeVersion(
                "v2");

        cache.get(
                context,
                action);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(
                String.class);

        verify(
                toolResultCache,
                org.mockito.Mockito.times(2))
                .get(
                        keyCaptor.capture());

        assertFalse(
                keyCaptor.getAllValues()
                        .get(0)
                        .equals(
                                keyCaptor.getAllValues()
                                        .get(1)));
    }

    @Test
    void successfulPutShouldWriteRawResult() {

        ToolExecutionContext context = ToolExecutionContext.sharedOnly();

        ToolAction action = action(
                "question");

        cache.put(
                context,
                action,
                "raw-result");

        verify(
                toolResultCache)
                .put(
                        anyString(),
                        eq(
                                "raw-result"));
    }

    @Test
    void blankResultShouldNotBeCached() {

        cache.put(
                ToolExecutionContext.sharedOnly(),
                action(
                        "question"),
                "   ");

        verify(
                toolResultCache,
                never())
                .put(
                        anyString(),
                        anyString());
    }

    @Test
    void nullResultShouldNotBeCached() {

        cache.put(
                ToolExecutionContext.sharedOnly(),
                action(
                        "question"),
                null);

        verify(
                toolResultCache,
                never())
                .put(
                        anyString(),
                        anyString());
    }

    private ToolAction action(
            String question) {

        return ToolAction.of(
                "task",
                LegalKnowledgeTool.TOOL_NAME,
                Map.of(
                        LegalKnowledgeTool.LEGAL_QUESTION_ARGUMENT,
                        question));
    }

    private ToolExecutionContext tenantContext(
            String tenantId,
            String goal) {

        TenantContext tenantContext = new TenantContext(
                tenantId,
                "user-1",
                "lawyer",
                Set.of(
                        UserRole.LAWYER));

        AgentContext agentContext = AgentContext.authenticated(
                goal == null
                        ? "fallback-goal"
                        : goal,
                tenantContext);

        /*
         * 如果要测试“没有 executionGoal 的 TENANT context”，
         * ToolExecutionContext 当前没有公开 factory 能表达这种状态。
         *
         * 正常 authenticated Agent execution 本身一定有 goal，
         * 所以 tenant 场景保留 executionGoal 是合理的。
         */
        return ToolExecutionContext.from(
                agentContext);
    }
}