package com.quince.lawyeraiassistant.agent.runtime.metrics;

import java.math.BigDecimal;

public record AgentPerformanceSnapshot(

        long totalDurationMs,

        int llmCalls,
        long llmDurationMs,

        long promptTokens,
        long completionTokens,
        long totalTokens,
        int usageMissingCalls,

        int toolCalls,
        long toolDurationMs,

        int mcpCalls,
        long mcpDurationMs,

        int cacheHits,
        int cacheMisses,

        long evidenceOriginalChars,
        long evidenceCompactedChars,

        int retries,
        int noProgressSuppressions,

        long llmCostMicros,
        long pricedLlmCalls,
        long pricingMissingCalls) {

    private static final BigDecimal MICROS_PER_USD = BigDecimal.valueOf(1_000_000L);

    public AgentPerformanceSnapshot {

        if (totalDurationMs < 0
                || llmCalls < 0
                || llmDurationMs < 0
                || promptTokens < 0
                || completionTokens < 0
                || totalTokens < 0
                || usageMissingCalls < 0
                || toolCalls < 0
                || toolDurationMs < 0
                || mcpCalls < 0
                || mcpDurationMs < 0
                || cacheHits < 0
                || cacheMisses < 0
                || evidenceOriginalChars < 0
                || evidenceCompactedChars < 0
                || retries < 0
                || noProgressSuppressions < 0
                || llmCostMicros < 0
                || pricedLlmCalls < 0
                || pricingMissingCalls < 0) {

            throw new IllegalArgumentException(
                    "Performance snapshot values must not be negative");
        }
    }

    /**
     * 兼容 22B Token Usage 阶段的调用方。
     */
    public AgentPerformanceSnapshot(
            long totalDurationMs,
            int llmCalls,
            long llmDurationMs,
            long promptTokens,
            long completionTokens,
            long totalTokens,
            int usageMissingCalls,
            int toolCalls,
            long toolDurationMs,
            int mcpCalls,
            long mcpDurationMs,
            int cacheHits,
            int cacheMisses,
            long evidenceOriginalChars,
            long evidenceCompactedChars,
            int retries,
            int noProgressSuppressions) {

        this(
                totalDurationMs,

                llmCalls,
                llmDurationMs,

                promptTokens,
                completionTokens,
                totalTokens,
                usageMissingCalls,

                toolCalls,
                toolDurationMs,

                mcpCalls,
                mcpDurationMs,

                cacheHits,
                cacheMisses,

                evidenceOriginalChars,
                evidenceCompactedChars,

                retries,
                noProgressSuppressions,

                0,
                0,
                0);
    }

    /**
     * 兼容 Token Usage 引入前的旧测试和调用方。
     */
    public AgentPerformanceSnapshot(
            long totalDurationMs,
            int llmCalls,
            long llmDurationMs,
            int toolCalls,
            long toolDurationMs,
            int mcpCalls,
            long mcpDurationMs,
            int cacheHits,
            int cacheMisses,
            long evidenceOriginalChars,
            long evidenceCompactedChars,
            int retries,
            int noProgressSuppressions) {

        this(
                totalDurationMs,

                llmCalls,
                llmDurationMs,

                0,
                0,
                0,
                0,

                toolCalls,
                toolDurationMs,

                mcpCalls,
                mcpDurationMs,

                cacheHits,
                cacheMisses,

                evidenceOriginalChars,
                evidenceCompactedChars,

                retries,
                noProgressSuppressions,

                0,
                0,
                0);
    }

    public double llmDurationRatio() {

        if (totalDurationMs <= 0) {
            return 0.0D;
        }

        return (double) llmDurationMs
                / totalDurationMs;
    }

    public double cacheHitRatio() {

        int total = cacheHits + cacheMisses;

        if (total == 0) {
            return 0.0D;
        }

        return (double) cacheHits
                / total;
    }

    public double evidenceReductionRatio() {

        if (evidenceOriginalChars <= 0) {
            return 0.0D;
        }

        return 1.0D
                - ((double) evidenceCompactedChars
                        / evidenceOriginalChars);
    }

    public double tokenUsageCoverageRatio() {

        if (llmCalls <= 0) {
            return 0.0D;
        }

        int availableCalls = Math.max(
                0,
                llmCalls - usageMissingCalls);

        return Math.min(
                1.0D,
                (double) availableCalls
                        / llmCalls);
    }

    public boolean hasCompleteTokenUsage() {

        return llmCalls > 0
                && usageMissingCalls == 0;
    }

    public BigDecimal llmCostUsd() {

        return BigDecimal.valueOf(
                llmCostMicros)
                .divide(MICROS_PER_USD);
    }

    public double pricingCoverageRatio() {

        long expectedCalls = pricedLlmCalls
                + pricingMissingCalls;

        if (expectedCalls == 0) {
            return 1.0D;
        }

        return (double) pricedLlmCalls
                / expectedCalls;
    }

    public boolean hasCompletePricing() {

        return pricedLlmCalls > 0
                && pricingMissingCalls == 0;
    }
}