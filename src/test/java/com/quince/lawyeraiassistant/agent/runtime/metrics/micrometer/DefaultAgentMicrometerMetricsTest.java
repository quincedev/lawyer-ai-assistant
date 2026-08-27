package com.quince.lawyeraiassistant.agent.runtime.metrics.micrometer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceSnapshot;
import com.quince.lawyeraiassistant.security.runtime.performance.PerformanceGuardrailResult;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.Test;

class DefaultAgentMicrometerMetricsTest {

        @Test
        void shouldRecordAgentPerformanceMetrics() {

                SimpleMeterRegistry registry = new SimpleMeterRegistry();

                DefaultAgentMicrometerMetrics metrics = new DefaultAgentMicrometerMetrics(
                                registry);

                AgentPerformanceSnapshot snapshot = new AgentPerformanceSnapshot(
                                120_000,

                                13,
                                119_000,

                                30_313,
                                15_177,
                                45_490,
                                0,

                                3,
                                680,

                                1,
                                660,

                                2,
                                1,

                                42_000,
                                16_000,

                                1,
                                1,

                                125_000,
                                13,
                                0);

                metrics.recordExecution(
                                snapshot);

                assertEquals(
                                1.0,
                                registry.counter(
                                                "agent.executions")
                                                .count());

                assertEquals(
                                13.0,
                                registry.counter(
                                                "agent.llm.calls")
                                                .count());

                assertEquals(
                                3.0,
                                registry.counter(
                                                "agent.tool.calls")
                                                .count());

                assertEquals(
                                1.0,
                                registry.counter(
                                                "agent.mcp.calls")
                                                .count());

                assertEquals(
                                2.0,
                                registry.counter(
                                                "agent.cache.hits")
                                                .count());

                assertEquals(
                                1.0,
                                registry.counter(
                                                "agent.cache.misses")
                                                .count());

                assertEquals(
                                1.0,
                                registry.counter(
                                                "agent.retries")
                                                .count());

                assertEquals(
                                1.0,
                                registry.counter(
                                                "agent.no_progress.suppressions")
                                                .count());

                assertEquals(
                                1,
                                registry.timer(
                                                "agent.execution.duration")
                                                .count());

                assertEquals(
                                120.0,
                                registry.timer(
                                                "agent.execution.duration")
                                                .totalTime(
                                                                java.util.concurrent.TimeUnit.SECONDS),
                                0.001);

                assertEquals(
                                30_313.0,
                                registry.counter(
                                                "agent.tokens.prompt")
                                                .count());

                assertEquals(
                                15_177.0,
                                registry.counter(
                                                "agent.tokens.completion")
                                                .count());

                assertEquals(
                                45_490.0,
                                registry.counter(
                                                "agent.tokens")
                                                .count());

                assertEquals(
                                0.125D,
                                registry.counter(
                                                "agent.cost.usd")
                                                .count(),
                                0.000001D);

                assertEquals(
                                0.0D,
                                registry.counter(
                                                "agent.cost.pricing.missing")
                                                .count());
        }

        @Test
        void shouldRecordGuardrailEvents() {

                SimpleMeterRegistry registry = new SimpleMeterRegistry();

                DefaultAgentMicrometerMetrics metrics = new DefaultAgentMicrometerMetrics(
                                registry);

                metrics.recordGuardrailResults(
                                java.util.List.of(
                                                PerformanceGuardrailResult.warn(
                                                                "llmCalls",
                                                                13,
                                                                12,
                                                                "warn"),
                                                PerformanceGuardrailResult.critical(
                                                                "totalDurationMs",
                                                                200000,
                                                                180000,
                                                                "critical")));

                assertEquals(
                                1.0,
                                registry.counter(
                                                "agent.performance.guardrail.events",
                                                "level",
                                                "WARN",
                                                "metric",
                                                "llmCalls")
                                                .count());

                assertEquals(
                                1.0,
                                registry.counter(
                                                "agent.performance.guardrail.events",
                                                "level",
                                                "CRITICAL",
                                                "metric",
                                                "totalDurationMs")
                                                .count());
        }
}