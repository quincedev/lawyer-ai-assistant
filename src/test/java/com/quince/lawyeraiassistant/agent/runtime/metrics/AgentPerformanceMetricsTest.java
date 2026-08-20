package com.quince.lawyeraiassistant.agent.runtime.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AgentPerformanceMetricsTest {

    @Test
    void shouldAggregatePerformanceMetrics() {

        AgentPerformanceMetrics metrics = new AgentPerformanceMetrics();

        metrics.recordLlmCall(100);
        metrics.recordLlmCall(200);

        metrics.recordToolCall(50);

        metrics.recordMcpCall(40);

        metrics.recordCacheHit();
        metrics.recordCacheMiss();

        metrics.recordEvidenceCompaction(
                10_000,
                4_000);

        metrics.recordRetry();

        metrics.recordNoProgressSuppression();

        AgentPerformanceSnapshot snapshot = metrics.snapshot();

        assertEquals(
                2,
                snapshot.llmCalls());

        assertEquals(
                300,
                snapshot.llmDurationMs());

        assertEquals(
                1,
                snapshot.toolCalls());

        assertEquals(
                50,
                snapshot.toolDurationMs());

        assertEquals(
                1,
                snapshot.mcpCalls());

        assertEquals(
                40,
                snapshot.mcpDurationMs());

        assertEquals(
                1,
                snapshot.cacheHits());

        assertEquals(
                1,
                snapshot.cacheMisses());

        assertEquals(
                0.5,
                snapshot.cacheHitRatio(),
                0.001);

        assertEquals(
                10_000,
                snapshot.evidenceOriginalChars());

        assertEquals(
                4_000,
                snapshot.evidenceCompactedChars());

        assertEquals(
                0.6,
                snapshot.evidenceReductionRatio(),
                0.001);

        assertEquals(
                1,
                snapshot.retries());

        assertEquals(
                1,
                snapshot.noProgressSuppressions());

        assertTrue(
                snapshot.totalDurationMs() >= 0);
    }

    @Test
    void shouldReturnZeroRatiosWhenNoDataExists() {

        AgentPerformanceSnapshot snapshot = new AgentPerformanceMetrics()
                .snapshot();

        assertEquals(
                0.0,
                snapshot.llmDurationRatio(),
                0.001);

        assertEquals(
                0.0,
                snapshot.cacheHitRatio(),
                0.001);

        assertEquals(
                0.0,
                snapshot.evidenceReductionRatio(),
                0.001);
    }

    @Test
    void shouldIgnoreNegativeDurationsAndSizes() {

        AgentPerformanceMetrics metrics = new AgentPerformanceMetrics();

        metrics.recordLlmCall(-100);
        metrics.recordToolCall(-50);
        metrics.recordMcpCall(-30);

        metrics.recordEvidenceCompaction(
                -1000,
                -500);

        AgentPerformanceSnapshot snapshot = metrics.snapshot();

        assertEquals(
                1,
                snapshot.llmCalls());

        assertEquals(
                0,
                snapshot.llmDurationMs());

        assertEquals(
                1,
                snapshot.toolCalls());

        assertEquals(
                0,
                snapshot.toolDurationMs());

        assertEquals(
                1,
                snapshot.mcpCalls());

        assertEquals(
                0,
                snapshot.mcpDurationMs());

        assertEquals(
                0,
                snapshot.evidenceOriginalChars());

        assertEquals(
                0,
                snapshot.evidenceCompactedChars());
    }
}