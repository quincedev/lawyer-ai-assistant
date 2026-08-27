package com.quince.lawyeraiassistant.agent.runtime.observability;

import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceMetrics;

import java.util.Objects;
import java.util.UUID;

public record AgentExecutionObservation(
        String executionId,
        long startedAtNanos,
        AgentPerformanceMetrics performanceMetrics) {

    public AgentExecutionObservation {

        if (executionId == null
                || executionId.isBlank()) {

            throw new IllegalArgumentException(
                    "executionId must not be blank");
        }

        Objects.requireNonNull(
                performanceMetrics,
                "performanceMetrics must not be null");

        executionId = executionId.trim();
    }

    /**
     * 向后兼容已有测试。
     */
    public AgentExecutionObservation(
            long startedAtNanos,
            AgentPerformanceMetrics performanceMetrics) {

        this(
                UUID.randomUUID().toString(),
                startedAtNanos,
                performanceMetrics);
    }
}