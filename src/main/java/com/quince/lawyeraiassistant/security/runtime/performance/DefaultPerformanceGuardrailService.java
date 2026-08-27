package com.quince.lawyeraiassistant.security.runtime.performance;

import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceSnapshot;
import com.quince.lawyeraiassistant.agent.runtime.performance.PerformanceGuardrailProperties;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@EnableConfigurationProperties(PerformanceGuardrailProperties.class)
public class DefaultPerformanceGuardrailService
        implements PerformanceGuardrailService {

    private final PerformanceGuardrailProperties properties;

    public DefaultPerformanceGuardrailService(
            PerformanceGuardrailProperties properties) {

        this.properties = Objects.requireNonNull(
                properties,
                "properties must not be null");

        validateCostThresholds(properties);
    }

    @Override
    public List<PerformanceGuardrailResult> evaluate(
            AgentPerformanceSnapshot snapshot) {

        Objects.requireNonNull(
                snapshot,
                "snapshot must not be null");

        List<PerformanceGuardrailResult> results = new ArrayList<>();

        evaluateThreshold(
                results,
                "llmCalls",
                snapshot.llmCalls(),
                properties.getWarnLlmCalls(),
                properties.getCriticalLlmCalls());

        evaluateThreshold(
                results,
                "totalDurationMs",
                snapshot.totalDurationMs(),
                properties.getWarnTotalDurationMs(),
                properties.getCriticalTotalDurationMs());

        if (snapshot.llmDurationMs() > properties.getWarnLlmDurationMs()) {

            results.add(
                    warn(
                            "llmDurationMs",
                            snapshot.llmDurationMs(),
                            properties.getWarnLlmDurationMs()));
        }

        if (snapshot.llmDurationRatio() > properties.getWarnLlmRatio()) {

            results.add(
                    warn(
                            "llmRatio",
                            snapshot.llmDurationRatio(),
                            properties.getWarnLlmRatio()));
        }

        evaluateInclusiveThreshold(
                results,
                "noProgressSuppressions",
                snapshot.noProgressSuppressions(),
                properties.getWarnNoProgressSuppressions(),
                properties.getCriticalNoProgressSuppressions());

        evaluateCacheHitRatio(
                results,
                snapshot);

        evaluateCostGuardrail(
                results,
                snapshot);

        return List.copyOf(results);
    }

    private void evaluateCostGuardrail(
            List<PerformanceGuardrailResult> results,
            AgentPerformanceSnapshot snapshot) {

        if (!properties.isCostEnabled()) {
            return;
        }

        /*
         * Usage 缺失时无法证明 Token 和 Cost 完整，
         * 因此单独产生 CRITICAL。
         */
        if (properties.isRequireCompleteTokenUsage()
                && snapshot.usageMissingCalls() > 0) {

            results.add(
                    PerformanceGuardrailResult.critical(
                            "usageMissingCalls",
                            snapshot.usageMissingCalls(),
                            0,
                            "LLM token usage is incomplete; "
                                    + "execution cost cannot be fully attributed"));
        }

        /*
         * Usage 存在但价格缺失时，成本会被低估。
         */
        if (properties.isRequireCompletePricing()
                && snapshot.pricingMissingCalls() > 0) {

            results.add(
                    PerformanceGuardrailResult.critical(
                            "pricingMissingCalls",
                            snapshot.pricingMissingCalls(),
                            0,
                            "Model pricing is missing; "
                                    + "execution cost is incomplete"));
        }

        BigDecimal actualCost = snapshot.llmCostUsd();

        BigDecimal criticalThreshold = properties.getCriticalExecutionCostUsd();

        BigDecimal warnThreshold = properties.getWarnExecutionCostUsd();

        if (actualCost.compareTo(
                criticalThreshold) >= 0) {

            results.add(
                    PerformanceGuardrailResult.critical(
                            "llmCostUsd",
                            actualCost.doubleValue(),
                            criticalThreshold.doubleValue(),
                            "Agent execution cost reached "
                                    + "the critical budget threshold"));

        } else if (actualCost.compareTo(
                warnThreshold) >= 0) {

            results.add(
                    PerformanceGuardrailResult.warn(
                            "llmCostUsd",
                            actualCost.doubleValue(),
                            warnThreshold.doubleValue(),
                            "Agent execution cost reached "
                                    + "the warning budget threshold"));
        }
    }

    private void evaluateThreshold(
            List<PerformanceGuardrailResult> results,
            String metric,
            double actual,
            double warnThreshold,
            double criticalThreshold) {

        if (actual > criticalThreshold) {

            results.add(
                    critical(
                            metric,
                            actual,
                            criticalThreshold));

        } else if (actual > warnThreshold) {

            results.add(
                    warn(
                            metric,
                            actual,
                            warnThreshold));
        }
    }

    private void evaluateInclusiveThreshold(
            List<PerformanceGuardrailResult> results,
            String metric,
            double actual,
            double warnThreshold,
            double criticalThreshold) {

        if (actual >= criticalThreshold) {

            results.add(
                    critical(
                            metric,
                            actual,
                            criticalThreshold));

        } else if (actual >= warnThreshold) {

            results.add(
                    warn(
                            metric,
                            actual,
                            warnThreshold));
        }
    }

    private PerformanceGuardrailResult warn(
            String metric,
            double actual,
            double threshold) {

        return PerformanceGuardrailResult.warn(
                metric,
                actual,
                threshold,
                "Performance warning threshold exceeded");
    }

    private PerformanceGuardrailResult critical(
            String metric,
            double actual,
            double threshold) {

        return PerformanceGuardrailResult.critical(
                metric,
                actual,
                threshold,
                "Performance critical threshold exceeded");
    }

    private void evaluateCacheHitRatio(
            List<PerformanceGuardrailResult> results,
            AgentPerformanceSnapshot snapshot) {

        int cacheAccesses = snapshot.cacheHits()
                + snapshot.cacheMisses();

        /*
         * Cold Run 样本过少时不判断命中率。
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
                            "Cache hit ratio is below "
                                    + "performance threshold"));
        }
    }

    private static void validateCostThresholds(
            PerformanceGuardrailProperties properties) {

        BigDecimal warn = Objects.requireNonNull(
                properties.getWarnExecutionCostUsd(),
                "warnExecutionCostUsd must not be null");

        BigDecimal critical = Objects.requireNonNull(
                properties.getCriticalExecutionCostUsd(),
                "criticalExecutionCostUsd must not be null");

        if (warn.signum() < 0) {
            throw new IllegalArgumentException(
                    "warnExecutionCostUsd must not be negative");
        }

        if (critical.signum() < 0) {
            throw new IllegalArgumentException(
                    "criticalExecutionCostUsd must not be negative");
        }

        if (critical.compareTo(warn) < 0) {
            throw new IllegalArgumentException(
                    "criticalExecutionCostUsd must be "
                            + "greater than or equal to "
                            + "warnExecutionCostUsd");
        }
    }
}