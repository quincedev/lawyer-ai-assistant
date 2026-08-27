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

    @Test
    void shouldReturnNoCostResultBelowWarnThreshold() {
        assertNoResult(service.evaluate(costSnapshot(10_000, 0, 2, 0)),
                "llmCostUsd");
    }

    @Test
    void shouldWarnAtCostWarnThreshold() {
        assertResult(service.evaluate(costSnapshot(20_000, 0, 2, 0)),
                "llmCostUsd", true);
    }

    @Test
    void shouldWarnBetweenCostWarnAndCriticalThresholds() {
        assertResult(service.evaluate(costSnapshot(30_000, 0, 2, 0)),
                "llmCostUsd", true);
    }

    @Test
    void shouldBeCriticalAtCostCriticalThreshold() {
        assertResult(service.evaluate(costSnapshot(50_000, 0, 2, 0)),
                "llmCostUsd", false);
    }

    @Test
    void shouldBeCriticalAboveCostCriticalThreshold() {
        assertResult(service.evaluate(costSnapshot(60_000, 0, 2, 0)),
                "llmCostUsd", false);
    }

    @Test
    void shouldBeCriticalWhenTokenUsageIsMissing() {
        assertResult(service.evaluate(costSnapshot(10_000, 1, 1, 0)),
                "usageMissingCalls", false);
    }

    @Test
    void shouldBeCriticalWhenPricingIsMissing() {
        assertResult(service.evaluate(costSnapshot(10_000, 0, 1, 1)),
                "pricingMissingCalls", false);
    }

    @Test
    void shouldNotEvaluateCostWhenCostGuardrailIsDisabled() {
        PerformanceGuardrailProperties properties = new PerformanceGuardrailProperties();
        properties.setCostEnabled(false);
        DefaultPerformanceGuardrailService disabledService =
                new DefaultPerformanceGuardrailService(properties);

        List<PerformanceGuardrailResult> results = disabledService.evaluate(
                costSnapshot(60_000, 1, 1, 1));

        assertNoResult(results, "llmCostUsd");
        assertNoResult(results, "usageMissingCalls");
        assertNoResult(results, "pricingMissingCalls");
    }

    @Test
    void shouldNotCheckUsageWhenCompleteTokenUsageIsNotRequired() {
        PerformanceGuardrailProperties properties = new PerformanceGuardrailProperties();
        properties.setRequireCompleteTokenUsage(false);
        DefaultPerformanceGuardrailService usageOptionalService =
                new DefaultPerformanceGuardrailService(properties);

        assertNoResult(
                usageOptionalService.evaluate(costSnapshot(10_000, 1, 1, 0)),
                "usageMissingCalls");
    }

    @Test
    void shouldNotCheckPricingWhenCompletePricingIsNotRequired() {
        PerformanceGuardrailProperties properties = new PerformanceGuardrailProperties();
        properties.setRequireCompletePricing(false);
        DefaultPerformanceGuardrailService pricingOptionalService =
                new DefaultPerformanceGuardrailService(properties);

        assertNoResult(
                pricingOptionalService.evaluate(costSnapshot(10_000, 0, 1, 1)),
                "pricingMissingCalls");
    }

    private void assertResult(List<PerformanceGuardrailResult> results,
            String metric, boolean warn) {
        assertTrue(results.stream().anyMatch(result ->
                result.metric().equals(metric)
                        && (warn ? result.isWarn() : result.isCritical())));
    }

    private void assertNoResult(List<PerformanceGuardrailResult> results,
            String metric) {
        assertTrue(results.stream().noneMatch(result ->
                result.metric().equals(metric)));
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

    private AgentPerformanceSnapshot costSnapshot(long costMicros,
            int usageMissingCalls, long pricedLlmCalls, long pricingMissingCalls) {
        return new AgentPerformanceSnapshot(
                10_000,
                2,
                9_000,
                2_000,
                1_000,
                3_000,
                usageMissingCalls,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                costMicros,
                pricedLlmCalls,
                pricingMissingCalls);
    }
}
