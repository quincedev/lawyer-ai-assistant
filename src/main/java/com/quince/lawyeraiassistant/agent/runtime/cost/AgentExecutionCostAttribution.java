package com.quince.lawyeraiassistant.agent.runtime.cost;

import com.quince.lawyeraiassistant.agent.model.AgentStatus;
import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceSnapshot;
import com.quince.lawyeraiassistant.agent.runtime.observability.AgentExecutionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record AgentExecutionCostAttribution(
        String executionId,
        String tenantId,
        AgentExecutionStatus executionStatus,
        AgentStatus agentStatus,
        Instant completedAt,
        long totalDurationMs,
        int llmCalls,
        long promptTokens,
        long completionTokens,
        long totalTokens,
        int usageMissingCalls,
        long pricedLlmCalls,
        long pricingMissingCalls,
        String currency,
        long costMicros) {

    private static final BigDecimal MICROS_PER_CURRENCY_UNIT = BigDecimal.valueOf(1_000_000L);

    public AgentExecutionCostAttribution {

        executionId = requireText(
                executionId,
                "executionId");

        tenantId = requireText(
                tenantId,
                "tenantId");

        currency = requireText(
                currency,
                "currency")
                .toUpperCase();

        Objects.requireNonNull(
                executionStatus,
                "executionStatus must not be null");

        Objects.requireNonNull(
                completedAt,
                "completedAt must not be null");

        if (totalDurationMs < 0
                || llmCalls < 0
                || promptTokens < 0
                || completionTokens < 0
                || totalTokens < 0
                || usageMissingCalls < 0
                || pricedLlmCalls < 0
                || pricingMissingCalls < 0
                || costMicros < 0) {

            throw new IllegalArgumentException(
                    "Attribution values must not be negative");
        }
    }

    public static AgentExecutionCostAttribution from(
            String executionId,
            String tenantId,
            AgentExecutionStatus executionStatus,
            AgentStatus agentStatus,
            String currency,
            AgentPerformanceSnapshot snapshot) {

        Objects.requireNonNull(
                snapshot,
                "snapshot must not be null");

        return new AgentExecutionCostAttribution(
                executionId,
                tenantId,
                executionStatus,
                agentStatus,
                Instant.now(),
                snapshot.totalDurationMs(),
                snapshot.llmCalls(),
                snapshot.promptTokens(),
                snapshot.completionTokens(),
                snapshot.totalTokens(),
                snapshot.usageMissingCalls(),
                snapshot.pricedLlmCalls(),
                snapshot.pricingMissingCalls(),
                currency,
                snapshot.llmCostMicros());
    }

    public BigDecimal cost() {

        return BigDecimal.valueOf(
                costMicros)
                .divide(
                        MICROS_PER_CURRENCY_UNIT);
    }

    public double tokenUsageCoverageRatio() {

        if (llmCalls <= 0) {
            return 0.0D;
        }

        long availableCalls = Math.max(
                0,
                llmCalls - usageMissingCalls);

        return Math.min(
                1.0D,
                (double) availableCalls
                        / llmCalls);
    }

    public double pricingCoverageRatio() {

        long expectedCalls = pricedLlmCalls
                + pricingMissingCalls;

        if (expectedCalls == 0) {
            return llmCalls == 0
                    ? 1.0D
                    : 0.0D;
        }

        return (double) pricedLlmCalls
                / expectedCalls;
    }

    public boolean fullyAttributed() {

        return llmCalls > 0
                && usageMissingCalls == 0
                && pricingMissingCalls == 0
                && pricedLlmCalls == llmCalls;
    }

    private static String requireText(
            String value,
            String field) {

        if (value == null
                || value.isBlank()) {

            throw new IllegalArgumentException(
                    field + " must not be blank");
        }

        return value.trim();
    }
}