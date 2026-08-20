package com.quince.lawyeraiassistant.agent.runtime.performance.regression;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceSnapshot;

@Component
public class DefaultAgentPerformanceRegressionEvaluator
        implements AgentPerformanceRegressionEvaluator {

    @Override
    public AgentPerformanceRegressionReport evaluate(
            AgentPerformanceSnapshot snapshot,
            AgentPerformanceBaseline baseline) {

        Objects.requireNonNull(
                snapshot,
                "AgentPerformanceSnapshot must not be null");

        Objects.requireNonNull(
                baseline,
                "AgentPerformanceBaseline must not be null");

        List<PerformanceRegressionMetricResult> results = new ArrayList<>();

        /*
         * ============================================
         * Structural metrics
         *
         * 这些指标相对稳定。
         * 一旦超过 baseline，直接认为发生性能/行为回归。
         * ============================================
         */

        evaluateMaximum(
                results,
                "llmCalls",
                snapshot.llmCalls(),
                baseline.maxLlmCalls());

        evaluateMaximum(
                results,
                "toolCalls",
                snapshot.toolCalls(),
                baseline.maxToolCalls());

        evaluateMaximum(
                results,
                "mcpCalls",
                snapshot.mcpCalls(),
                baseline.maxMcpCalls());

        evaluateMaximum(
                results,
                "retries",
                snapshot.retries(),
                baseline.maxRetries());

        evaluateMaximum(
                results,
                "noProgressSuppressions",
                snapshot.noProgressSuppressions(),
                baseline.maxNoProgressSuppressions());

        /*
         * ============================================
         * Cache
         * ============================================
         */

        evaluateCacheHitRatio(
                results,
                snapshot,
                baseline);

        /*
         * ============================================
         * Evidence
         * ============================================
         */

        evaluateMinimum(
                results,
                "evidenceReductionRatio",
                snapshot.evidenceReductionRatio(),
                baseline.minEvidenceReductionRatio());

        /*
         * ============================================
         * Latency
         *
         * LLM latency 本身有明显波动，
         * 因此 totalDuration 不采用简单 max threshold。
         * ============================================
         */

        evaluateLatency(
                results,
                snapshot.totalDurationMs(),
                baseline);

        PerformanceRegressionLevel overall = resolveOverallLevel(
                results);

        return new AgentPerformanceRegressionReport(
                overall,
                results);
    }

    private void evaluateMaximum(
            List<PerformanceRegressionMetricResult> results,
            String metric,
            double actual,
            double maximum) {

        if (actual > maximum) {

            results.add(
                    new PerformanceRegressionMetricResult(
                            metric,
                            PerformanceRegressionLevel.FAIL,
                            actual,
                            maximum,
                            "Metric exceeded regression baseline"));

            return;
        }

        results.add(
                new PerformanceRegressionMetricResult(
                        metric,
                        PerformanceRegressionLevel.PASS,
                        actual,
                        maximum,
                        "Metric is within regression baseline"));
    }

    private void evaluateMinimum(
            List<PerformanceRegressionMetricResult> results,
            String metric,
            double actual,
            double minimum) {

        if (actual < minimum) {

            results.add(
                    new PerformanceRegressionMetricResult(
                            metric,
                            PerformanceRegressionLevel.FAIL,
                            actual,
                            minimum,
                            "Metric fell below regression baseline"));

            return;
        }

        results.add(
                new PerformanceRegressionMetricResult(
                        metric,
                        PerformanceRegressionLevel.PASS,
                        actual,
                        minimum,
                        "Metric is within regression baseline"));
    }

    private void evaluateCacheHitRatio(
            List<PerformanceRegressionMetricResult> results,
            AgentPerformanceSnapshot snapshot,
            AgentPerformanceBaseline baseline) {

        int cacheAccesses = snapshot.cacheHits()
                + snapshot.cacheMisses();

        /*
         * 没有发生 Cache Access，
         * 不能把 0.0 当作 Cache Regression。
         */
        if (cacheAccesses == 0) {

            results.add(
                    new PerformanceRegressionMetricResult(
                            "cacheHitRatio",
                            PerformanceRegressionLevel.PASS,
                            0.0,
                            baseline.minCacheHitRatio(),
                            "No cache access occurred"));

            return;
        }

        evaluateMinimum(
                results,
                "cacheHitRatio",
                snapshot.cacheHitRatio(),
                baseline.minCacheHitRatio());
    }

    private void evaluateLatency(
            List<PerformanceRegressionMetricResult> results,
            long actualDurationMs,
            AgentPerformanceBaseline baseline) {

        double ratio = (double) actualDurationMs
                / baseline.baselineTotalDurationMs();

        if (ratio >= baseline.failLatencyRegressionRatio()) {

            results.add(
                    new PerformanceRegressionMetricResult(
                            "totalDurationMs",
                            PerformanceRegressionLevel.FAIL,
                            actualDurationMs,
                            baseline.baselineTotalDurationMs(),
                            "Latency exceeded fail regression ratio"));

            return;
        }

        if (ratio >= baseline.warnLatencyRegressionRatio()) {

            results.add(
                    new PerformanceRegressionMetricResult(
                            "totalDurationMs",
                            PerformanceRegressionLevel.WARN,
                            actualDurationMs,
                            baseline.baselineTotalDurationMs(),
                            "Latency exceeded warning regression ratio"));

            return;
        }

        results.add(
                new PerformanceRegressionMetricResult(
                        "totalDurationMs",
                        PerformanceRegressionLevel.PASS,
                        actualDurationMs,
                        baseline.baselineTotalDurationMs(),
                        "Latency is within regression baseline"));
    }

    private PerformanceRegressionLevel resolveOverallLevel(
            List<PerformanceRegressionMetricResult> results) {

        boolean hasFailure = results.stream()
                .anyMatch(
                        PerformanceRegressionMetricResult::isFail);

        if (hasFailure) {

            return PerformanceRegressionLevel.FAIL;
        }

        boolean hasWarning = results.stream()
                .anyMatch(
                        PerformanceRegressionMetricResult::isWarn);

        if (hasWarning) {

            return PerformanceRegressionLevel.WARN;
        }

        return PerformanceRegressionLevel.PASS;
    }
}