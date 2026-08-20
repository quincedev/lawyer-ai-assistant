package com.quince.lawyeraiassistant.security.runtime.performance;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceSnapshot;
import com.quince.lawyeraiassistant.agent.runtime.performance.PerformanceGuardrailProperties;

class DefaultPerformanceGuardrailServiceTest {

    private DefaultPerformanceGuardrailService service;

    @BeforeEach
    void setUp() {
        service = new DefaultPerformanceGuardrailService(
                new PerformanceGuardrailProperties());
    }

    @Test
    void shouldReturnNoResultForNormalSnapshot() {
        assertTrue(service.evaluate(snapshot(1_000, 1, 500, 0)).isEmpty());
    }

    @Test
    void shouldWarnForThirteenLlmCalls() {
        assertResult(service.evaluate(snapshot(1_000, 13, 500, 0)), "llmCalls", true);
    }

    @Test
    void shouldBeCriticalForSixteenLlmCalls() {
        assertResult(service.evaluate(snapshot(1_000, 16, 500, 0)), "llmCalls", false);
    }

    @Test
    void shouldWarnForOneHundredFortySixSecondDuration() {
        assertResult(service.evaluate(snapshot(146_000, 1, 1_000, 0)),
                "totalDurationMs", true);
    }

    @Test
    void shouldBeCriticalForOneHundredEightyOneSecondDuration() {
        assertResult(service.evaluate(snapshot(181_000, 1, 1_000, 0)),
                "totalDurationMs", false);
    }

    @Test
    void shouldWarnWhenLlmRatioExceedsThreshold() {
        assertResult(service.evaluate(snapshot(100_000, 1, 96_000, 0)),
                "llmRatio", true);
    }

    @Test
    void shouldWarnForOneNoProgressSuppression() {
        assertResult(service.evaluate(snapshot(1_000, 1, 500, 1)),
                "noProgressSuppressions", true);
    }

    @Test
    void shouldBeCriticalForTwoNoProgressSuppressions() {
        assertResult(service.evaluate(snapshot(1_000, 1, 500, 2)),
                "noProgressSuppressions", false);
    }

    @Test
    void shouldIgnoreCacheRatioWithOnlyOneAccess() {
        assertTrue(service.evaluate(snapshotWithCache(0, 1)).stream()
                .noneMatch(result -> result.metric().equals("cacheHitRatio")));
    }

    @Test
    void shouldWarnForLowCacheHitRatioWithEnoughAccesses() {
        assertResult(service.evaluate(snapshotWithCache(0, 2)),
                "cacheHitRatio", true);
    }

    private void assertResult(List<PerformanceGuardrailResult> results,
            String metric, boolean warn) {
        assertTrue(results.stream().anyMatch(result ->
                result.metric().equals(metric)
                        && (warn ? result.isWarn() : result.isCritical())));
    }

    private AgentPerformanceSnapshot snapshot(long totalDurationMs,
            int llmCalls, long llmDurationMs, int noProgressSuppressions) {
        return new AgentPerformanceSnapshot(
                totalDurationMs,
                llmCalls,
                llmDurationMs,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                noProgressSuppressions);
    }

    private AgentPerformanceSnapshot snapshotWithCache(int cacheHits, int cacheMisses) {
        return new AgentPerformanceSnapshot(
                1_000,
                1,
                500,
                0,
                0,
                0,
                0,
                cacheHits,
                cacheMisses,
                0,
                0,
                0,
                0);
    }
}
