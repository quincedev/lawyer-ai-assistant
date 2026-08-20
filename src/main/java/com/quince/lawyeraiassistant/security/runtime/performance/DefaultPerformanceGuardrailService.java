package com.quince.lawyeraiassistant.security.runtime.performance;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceSnapshot;
import com.quince.lawyeraiassistant.agent.runtime.performance.PerformanceGuardrailProperties;

@Component
@EnableConfigurationProperties(PerformanceGuardrailProperties.class)
public class DefaultPerformanceGuardrailService implements PerformanceGuardrailService {

    private final PerformanceGuardrailProperties properties;

    public DefaultPerformanceGuardrailService(PerformanceGuardrailProperties properties) {
        this.properties = Objects.requireNonNull(
                properties,
                "PerformanceGuardrailProperties must not be null");
    }

    @Override
    public List<PerformanceGuardrailResult> evaluate(AgentPerformanceSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "AgentPerformanceSnapshot must not be null");

        List<PerformanceGuardrailResult> results = new ArrayList<>();
        evaluateThreshold(results, "llmCalls", snapshot.llmCalls(),
                properties.getWarnLlmCalls(), properties.getCriticalLlmCalls());
        evaluateThreshold(results, "totalDurationMs", snapshot.totalDurationMs(),
                properties.getWarnTotalDurationMs(), properties.getCriticalTotalDurationMs());

        if (snapshot.llmDurationMs() > properties.getWarnLlmDurationMs()) {
            results.add(warn("llmDurationMs", snapshot.llmDurationMs(),
                    properties.getWarnLlmDurationMs()));
        }
        if (snapshot.llmDurationRatio() > properties.getWarnLlmRatio()) {
            results.add(warn("llmRatio", snapshot.llmDurationRatio(),
                    properties.getWarnLlmRatio()));
        }

        evaluateInclusiveThreshold(results, "noProgressSuppressions",
                snapshot.noProgressSuppressions(),
                properties.getWarnNoProgressSuppressions(),
                properties.getCriticalNoProgressSuppressions());

        evaluateCacheHitRatio(results, snapshot);

        return List.copyOf(results);
    }

    private void evaluateThreshold(List<PerformanceGuardrailResult> results,
            String metric, double actual, double warnThreshold, double criticalThreshold) {
        if (actual > criticalThreshold) {
            results.add(critical(metric, actual, criticalThreshold));
        } else if (actual > warnThreshold) {
            results.add(warn(metric, actual, warnThreshold));
        }
    }

    private void evaluateInclusiveThreshold(List<PerformanceGuardrailResult> results,
            String metric, double actual, double warnThreshold, double criticalThreshold) {
        if (actual >= criticalThreshold) {
            results.add(critical(metric, actual, criticalThreshold));
        } else if (actual >= warnThreshold) {
            results.add(warn(metric, actual, warnThreshold));
        }
    }

    private PerformanceGuardrailResult warn(String metric, double actual, double threshold) {
        return PerformanceGuardrailResult.warn(
                metric, actual, threshold, "Performance warning threshold exceeded");
    }

    private PerformanceGuardrailResult critical(String metric, double actual, double threshold) {
        return PerformanceGuardrailResult.critical(
                metric, actual, threshold, "Performance critical threshold exceeded");
    }

    private void evaluateCacheHitRatio(
            List<PerformanceGuardrailResult> results,
            AgentPerformanceSnapshot snapshot) {

        int cacheAccesses = snapshot.cacheHits()
                + snapshot.cacheMisses();

        /*
         * 样本过少时不判断 Cache Hit Ratio。
         *
         * 例如 Cold Run：
         *
         * hit=0
         * miss=1
         *
         * 这是正常首次访问，
         * 不能因此认为 Cache 性能异常。
         */
        if (cacheAccesses < 2) {

            return;
        }

        double actual = snapshot.cacheHitRatio();

        double threshold = properties.getWarnLowCacheHitRatio();

        if (actual < threshold) {

            results.add(
                    PerformanceGuardrailResult.warn(
                            "cacheHitRatio",
                            actual,
                            threshold,
                            "Cache hit ratio is below performance threshold"));
        }
    }
}
