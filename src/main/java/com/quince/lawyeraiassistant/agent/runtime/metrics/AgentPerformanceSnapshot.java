package com.quince.lawyeraiassistant.agent.runtime.metrics;

public record AgentPerformanceSnapshot(

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

    public double llmDurationRatio() {

        if (totalDurationMs <= 0) {

            return 0.0;
        }

        return (double) llmDurationMs
                / totalDurationMs;
    }

    public double cacheHitRatio() {

        int total = cacheHits
                + cacheMisses;

        if (total == 0) {

            return 0.0;
        }

        return (double) cacheHits
                / total;
    }

    public double evidenceReductionRatio() {

        if (evidenceOriginalChars <= 0) {

            return 0.0;
        }

        return 1.0
                - ((double) evidenceCompactedChars
                        / evidenceOriginalChars);
    }
}