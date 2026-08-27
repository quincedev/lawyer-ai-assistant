package com.quince.lawyeraiassistant.evaluation.metrics;

import com.quince.lawyeraiassistant.evaluation.model.AgentEvaluationResult;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MicrometerAgentEvaluationMetricsTest {

    private SimpleMeterRegistry registry;

    private MicrometerAgentEvaluationMetrics metrics;

    @BeforeEach
    void setUp() {

        registry = new SimpleMeterRegistry();

        metrics = new MicrometerAgentEvaluationMetrics(
                registry);
    }

    @Test
    void shouldPreRegisterStableEvaluationMeters() {

        assertEquals(
                0.0D,
                registry.counter(
                        "agent.evaluations",
                        "evaluation_type",
                        "deterministic",
                        "result",
                        "pass")
                        .count());

        assertEquals(
                0.0D,
                registry.counter(
                        "agent.evaluations",
                        "evaluation_type",
                        "deterministic",
                        "result",
                        "fail")
                        .count());

        assertEquals(
                0L,
                registry.summary(
                        "agent.evaluation.score",
                        "evaluation_type",
                        "deterministic")
                        .count());
    }

    @Test
    void shouldRecordPassedEvaluation() {

        metrics.record(
                result(
                        82.5D,
                        true));

        assertEquals(
                1.0D,
                registry.counter(
                        "agent.evaluations",
                        "evaluation_type",
                        "deterministic",
                        "result",
                        "pass")
                        .count());

        assertEquals(
                0.0D,
                registry.counter(
                        "agent.evaluations",
                        "evaluation_type",
                        "deterministic",
                        "result",
                        "fail")
                        .count());

        assertEquals(
                1L,
                registry.summary(
                        "agent.evaluation.score",
                        "evaluation_type",
                        "deterministic")
                        .count());

        assertEquals(
                82.5D,
                registry.summary(
                        "agent.evaluation.score",
                        "evaluation_type",
                        "deterministic")
                        .totalAmount(),
                0.000001D);
    }

    @Test
    void shouldRecordFailedEvaluation() {

        metrics.record(
                result(
                        62.0D,
                        false));

        assertEquals(
                1.0D,
                registry.counter(
                        "agent.evaluations",
                        "evaluation_type",
                        "deterministic",
                        "result",
                        "fail")
                        .count());
    }

    private AgentEvaluationResult result(
            double score,
            boolean passed) {

        return new AgentEvaluationResult(
                "test-case",
                score,
                75.0D,
                passed,
                0.8D,
                List.of("赔偿金"),
                List.of(),
                List.of(),
                List.of());
    }
}