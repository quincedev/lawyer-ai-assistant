package com.quince.lawyeraiassistant.agent.runtime.observability;

import java.util.Objects;

import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceMetrics;

public record AgentExecutionObservation(
        long startedAtNanos,
        AgentPerformanceMetrics performanceMetrics) {

    public AgentExecutionObservation {

        Objects.requireNonNull(
                performanceMetrics,
                "performanceMetrics must not be null");
    }
}