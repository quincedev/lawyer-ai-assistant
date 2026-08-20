package com.quince.lawyeraiassistant.agent.runtime.performance.regression;

public record AgentPerformanceBaseline(

        int maxLlmCalls,

        int maxToolCalls,

        int maxMcpCalls,

        int maxRetries,

        int maxNoProgressSuppressions,

        double minCacheHitRatio,

        double minEvidenceReductionRatio,

        long baselineTotalDurationMs,

        double warnLatencyRegressionRatio,

        double failLatencyRegressionRatio) {

    public AgentPerformanceBaseline {

        if (maxLlmCalls < 0) {
            throw new IllegalArgumentException(
                    "maxLlmCalls must not be negative");
        }

        if (maxToolCalls < 0) {
            throw new IllegalArgumentException(
                    "maxToolCalls must not be negative");
        }

        if (maxMcpCalls < 0) {
            throw new IllegalArgumentException(
                    "maxMcpCalls must not be negative");
        }

        if (maxRetries < 0) {
            throw new IllegalArgumentException(
                    "maxRetries must not be negative");
        }

        if (maxNoProgressSuppressions < 0) {
            throw new IllegalArgumentException(
                    "maxNoProgressSuppressions must not be negative");
        }

        if (minCacheHitRatio < 0.0
                || minCacheHitRatio > 1.0) {

            throw new IllegalArgumentException(
                    "minCacheHitRatio must be between 0 and 1");
        }

        if (minEvidenceReductionRatio < 0.0
                || minEvidenceReductionRatio > 1.0) {

            throw new IllegalArgumentException(
                    "minEvidenceReductionRatio must be between 0 and 1");
        }

        if (baselineTotalDurationMs <= 0) {
            throw new IllegalArgumentException(
                    "baselineTotalDurationMs must be positive");
        }

        if (warnLatencyRegressionRatio < 1.0) {
            throw new IllegalArgumentException(
                    "warnLatencyRegressionRatio must be >= 1");
        }

        if (failLatencyRegressionRatio < warnLatencyRegressionRatio) {

            throw new IllegalArgumentException(
                    "failLatencyRegressionRatio must be >= warnLatencyRegressionRatio");
        }
    }
}