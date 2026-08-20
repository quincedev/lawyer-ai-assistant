package com.quince.lawyeraiassistant.agent.runtime.performance.regression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceSnapshot;

class DefaultAgentPerformanceRegressionEvaluatorTest {

    private DefaultAgentPerformanceRegressionEvaluator evaluator;

    @BeforeEach
    void setUp() {

        evaluator = new DefaultAgentPerformanceRegressionEvaluator();
    }

    /*
     * ============================================
     * Case 1
     *
     * 所有指标均处于 baseline 内。
     *
     * Expected:
     * PASS
     * ============================================
     */
    @Test
    void shouldPassWhenMetricsAreWithinBaseline() {

        AgentPerformanceSnapshot snapshot = snapshot(
                100_000,
                12,
                2,
                1,
                2,
                1,
                38_000,
                14_000,
                1,
                0);

        AgentPerformanceRegressionReport report = evaluator.evaluate(
                snapshot,
                coldBaseline());

        assertEquals(
                PerformanceRegressionLevel.PASS,
                report.overallLevel());

        assertTrue(
                report.passed());

        assertFalse(
                report.hasWarnings());

        assertFalse(
                report.hasFailures());
    }

    /*
     * ============================================
     * Case 2
     *
     * LLM Call 数超过 baseline。
     *
     * Structural Regression
     *
     * Expected:
     * FAIL
     * ============================================
     */
    @Test
    void shouldFailWhenLlmCallsRegress() {

        AgentPerformanceSnapshot snapshot = snapshot(
                100_000,

                // baseline = 13
                14,

                2,
                1,
                2,
                1,
                38_000,
                14_000,
                1,
                0);

        AgentPerformanceRegressionReport report = evaluator.evaluate(
                snapshot,
                coldBaseline());

        assertFailMetric(
                report,
                "llmCalls");
    }

    /*
     * ============================================
     * Case 3
     *
     * Tool Call 数增加。
     *
     * Expected:
     * FAIL
     * ============================================
     */
    @Test
    void shouldFailWhenToolCallsRegress() {

        AgentPerformanceSnapshot snapshot = snapshot(
                100_000,
                12,

                // baseline = 3
                4,

                1,
                2,
                1,
                38_000,
                14_000,
                1,
                0);

        AgentPerformanceRegressionReport report = evaluator.evaluate(
                snapshot,
                coldBaseline());

        assertFailMetric(
                report,
                "toolCalls");
    }

    /*
     * ============================================
     * Case 4
     *
     * Cold baseline:
     *
     * MCP <= 1
     *
     * 如果变成 2，
     * 说明 Tool / Cache / Retry 路径可能发生回归。
     *
     * Expected:
     * FAIL
     * ============================================
     */
    @Test
    void shouldFailWhenColdMcpCallsRegress() {

        AgentPerformanceSnapshot snapshot = snapshot(
                100_000,
                12,
                2,

                // baseline = 1
                2,

                2,
                1,
                38_000,
                14_000,
                1,
                0);

        AgentPerformanceRegressionReport report = evaluator.evaluate(
                snapshot,
                coldBaseline());

        assertFailMetric(
                report,
                "mcpCalls");
    }

    /*
     * ============================================
     * Case 5
     *
     * Warm baseline 最关键的 Regression Gate。
     *
     * Warm Cache 正常情况下：
     *
     * MCP = 0
     *
     * 如果突然出现 MCP 调用，
     * 说明 Cache Key / Cache Lookup 可能被改坏。
     *
     * Expected:
     * FAIL
     * ============================================
     */
    @Test
    void shouldFailWhenWarmExecutionCallsMcp() {

        AgentPerformanceSnapshot snapshot = snapshot(
                100_000,
                12,
                1,

                // Warm baseline = 0
                1,

                1,
                0,
                19_000,
                7_000,
                0,
                0);

        AgentPerformanceRegressionReport report = evaluator.evaluate(
                snapshot,
                warmBaseline());

        assertFailMetric(
                report,
                "mcpCalls");
    }

    /*
     * ============================================
     * Case 6
     *
     * Retry Regression
     *
     * baseline = 1
     *
     * Expected:
     * FAIL
     * ============================================
     */
    @Test
    void shouldFailWhenRetriesRegress() {

        AgentPerformanceSnapshot snapshot = snapshot(
                100_000,
                12,
                2,
                1,
                2,
                1,
                38_000,
                14_000,

                // baseline = 1
                2,

                0);

        AgentPerformanceRegressionReport report = evaluator.evaluate(
                snapshot,
                coldBaseline());

        assertFailMetric(
                report,
                "retries");
    }

    /*
     * ============================================
     * Case 7
     *
     * Evidence Compaction Regression
     *
     * 10000 -> 8000
     *
     * Reduction = 20%
     *
     * baseline >= 55%
     *
     * Expected:
     * FAIL
     * ============================================
     */
    @Test
    void shouldFailWhenEvidenceCompactionRegresses() {

        AgentPerformanceSnapshot snapshot = snapshot(
                100_000,
                12,
                2,
                1,
                2,
                1,

                10_000,
                8_000,

                1,
                0);

        AgentPerformanceRegressionReport report = evaluator.evaluate(
                snapshot,
                coldBaseline());

        assertFailMetric(
                report,
                "evidenceReductionRatio");
    }

    /*
     * ============================================
     * Case 8
     *
     * Cache Hit Ratio Regression
     *
     * hit = 1
     * miss = 3
     *
     * ratio = 25%
     *
     * Cold baseline >= 50%
     *
     * Expected:
     * FAIL
     * ============================================
     */
    @Test
    void shouldFailWhenCacheHitRatioRegresses() {

        AgentPerformanceSnapshot snapshot = snapshot(
                100_000,
                12,
                2,
                1,

                1,
                3,

                38_000,
                14_000,
                1,
                0);

        AgentPerformanceRegressionReport report = evaluator.evaluate(
                snapshot,
                coldBaseline());

        assertFailMetric(
                report,
                "cacheHitRatio");
    }

    /*
     * ============================================
     * Case 9
     *
     * Moderate latency regression.
     *
     * baseline = 120s
     *
     * current = 170s
     *
     * ratio = 1.416
     *
     * warn = 1.30
     * fail = 1.75
     *
     * Expected:
     * WARN
     *
     * Release Gate 仍然允许通过。
     * ============================================
     */
    @Test
    void shouldWarnForModerateLatencyRegression() {

        AgentPerformanceSnapshot snapshot = snapshot(
                170_000,
                12,
                2,
                1,
                2,
                1,
                38_000,
                14_000,
                1,
                0);

        AgentPerformanceRegressionReport report = evaluator.evaluate(
                snapshot,
                coldBaseline());

        assertEquals(
                PerformanceRegressionLevel.WARN,
                report.overallLevel());

        assertTrue(
                report.passed());

        assertTrue(
                report.hasWarnings());

        assertFalse(
                report.hasFailures());

        assertTrue(
                report.results()
                        .stream()
                        .anyMatch(
                                result -> result.metric()
                                        .equals(
                                                "totalDurationMs")
                                        && result.isWarn()));
    }

    /*
     * ============================================
     * Case 10
     *
     * Severe latency regression.
     *
     * baseline = 120s
     *
     * current = 220s
     *
     * ratio = 1.833
     *
     * > 1.75
     *
     * Expected:
     * FAIL
     * ============================================
     */
    @Test
    void shouldFailForSevereLatencyRegression() {

        AgentPerformanceSnapshot snapshot = snapshot(
                220_000,
                12,
                2,
                1,
                2,
                1,
                38_000,
                14_000,
                1,
                0);

        AgentPerformanceRegressionReport report = evaluator.evaluate(
                snapshot,
                coldBaseline());

        assertFailMetric(
                report,
                "totalDurationMs");
    }

    /*
     * ============================================
     * Case 11
     *
     * 没有发生 Cache Access。
     *
     * 不能把 cacheHitRatio=0
     * 错误判断成 Regression。
     *
     * Expected:
     * PASS
     * ============================================
     */
    @Test
    void shouldNotFailWhenNoCacheAccessOccurred() {

        AgentPerformanceSnapshot snapshot = snapshot(
                100_000,
                12,
                2,
                1,

                0,
                0,

                38_000,
                14_000,
                1,
                0);

        AgentPerformanceRegressionReport report = evaluator.evaluate(
                snapshot,
                coldBaseline());

        assertTrue(
                report.results()
                        .stream()
                        .anyMatch(
                                result -> result.metric()
                                        .equals(
                                                "cacheHitRatio")
                                        && result.isPass()));
    }

    /*
     * ============================================
     * Case 12
     *
     * Warm happy path。
     *
     * MCP = 0
     * Cache Hit Ratio = 100%
     *
     * Expected:
     * PASS
     * ============================================
     */
    @Test
    void shouldPassWarmExecutionWhenCacheIsStable() {

        AgentPerformanceSnapshot snapshot = snapshot(
                110_000,
                13,
                1,

                // Warm 不允许 MCP
                0,

                // 100% Cache HIT
                1,
                0,

                19_101,
                7_279,

                0,
                0);

        AgentPerformanceRegressionReport report = evaluator.evaluate(
                snapshot,
                warmBaseline());

        assertEquals(
                PerformanceRegressionLevel.PASS,
                report.overallLevel());

        assertTrue(
                report.passed());

        assertFalse(
                report.hasFailures());
    }

    /*
     * ============================================
     * Cold Baseline
     * ============================================
     */
    private AgentPerformanceBaseline coldBaseline() {

        return new AgentPerformanceBaseline(
                13,
                3,
                1,
                1,
                1,
                0.50,
                0.55,
                120_000,
                1.30,
                1.75);
    }

    /*
     * ============================================
     * Warm Baseline
     * ============================================
     */
    private AgentPerformanceBaseline warmBaseline() {

        return new AgentPerformanceBaseline(
                13,
                2,

                // Warm execution must not call MCP
                0,

                1,
                1,

                // Warm cache expected >= 90%
                0.90,

                0.55,

                120_000,
                1.30,
                1.75);
    }

    /*
     * ============================================
     * Snapshot Factory
     *
     * 不重要的 duration 使用固定值，
     * 让每个 Case 只突出自己测试的变量。
     * ============================================
     */
    private AgentPerformanceSnapshot snapshot(
            long totalDurationMs,
            int llmCalls,
            int toolCalls,
            int mcpCalls,
            int cacheHits,
            int cacheMisses,
            long evidenceOriginalChars,
            long evidenceCompactedChars,
            int retries,
            int noProgressSuppressions) {

        return new AgentPerformanceSnapshot(
                totalDurationMs,

                llmCalls,
                Math.max(
                        0,
                        totalDurationMs - 1_000),

                toolCalls,
                500,

                mcpCalls,
                400,

                cacheHits,
                cacheMisses,

                evidenceOriginalChars,
                evidenceCompactedChars,

                retries,
                noProgressSuppressions);
    }

    private void assertFailMetric(
            AgentPerformanceRegressionReport report,
            String metric) {

        assertEquals(
                PerformanceRegressionLevel.FAIL,
                report.overallLevel());

        assertFalse(
                report.passed());

        assertTrue(
                report.hasFailures());

        assertTrue(
                report.results()
                        .stream()
                        .anyMatch(
                                result -> result.metric()
                                        .equals(metric)
                                        && result.isFail()));
    }
}