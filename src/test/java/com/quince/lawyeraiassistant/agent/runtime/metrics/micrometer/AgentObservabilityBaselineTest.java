package com.quince.lawyeraiassistant.agent.runtime.metrics.micrometer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceSnapshot;
import com.quince.lawyeraiassistant.evaluation.judge.metrics.MicrometerModelJudgeMetrics;
import com.quince.lawyeraiassistant.evaluation.metrics.MicrometerAgentEvaluationMetrics;
import com.quince.lawyeraiassistant.security.runtime.performance.PerformanceGuardrailResult;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Locks the existing Agent observability contract.
 *
 * <p>
 * These metric names are consumed externally by:
 * </p>
 *
 * <ul>
 * <li>Prometheus</li>
 * <li>Grafana</li>
 * <li>release regression scripts</li>
 * </ul>
 *
 * <p>
 * Metric names are intentionally hard-coded in this test instead
 * of being imported from production constants. Renaming a production
 * metric must therefore explicitly update this contract and all
 * external consumers.
 * </p>
 */
class AgentObservabilityBaselineTest {

        private static final Set<String> BASELINE_METERS = Set.of(
                        "agent.executions",
                        "agent.llm.calls",
                        "agent.tool.calls",
                        "agent.mcp.calls",

                        "agent.cache.hits",
                        "agent.cache.misses",

                        "agent.retries",
                        "agent.no_progress.suppressions",

                        "agent.execution.duration",
                        "agent.llm.duration",
                        "agent.tool.duration",
                        "agent.mcp.duration",

                        "agent.evidence.original.chars",
                        "agent.evidence.compacted.chars",

                        "agent.tokens.prompt",
                        "agent.tokens.completion",
                        "agent.tokens",
                        "agent.tokens.usage.missing",

                        "agent.cost.usd",
                        "agent.cost.pricing.missing",
                        "agent.evaluations",
                        "agent.evaluation.score",

                        "agent.evaluation.judge.calls",
                        "agent.evaluation.judge.failures",
                        "agent.evaluation.judge.score",
                        "agent.evaluation.judge.duration",
                        "agent.evaluation.judge.prompt.tokens",
                        "agent.evaluation.judge.completion.tokens",
                        "agent.evaluation.judge.total.tokens",
                        "agent.evaluation.judge.usage.missing",
                        "agent.evaluation.judge.cost.usd",
                        "agent.evaluation.judge.pricing.missing");

        private static final Set<String> FORBIDDEN_TAG_KEYS = Set.of(
                        "tenant",
                        "tenantid",
                        "tenant_id",
                        "userid",
                        "user_id",
                        "username",
                        "question",
                        "goal",
                        "conversationid",
                        "conversation_id",
                        "executionid",
                        "execution_id",
                        "requestid",
                        "request_id",
                        "token",
                        "accesstoken",
                        "access_token",
                        "password",
                        "apikey",
                        "api_key");

        private SimpleMeterRegistry registry;

        private DefaultAgentMicrometerMetrics metrics;

        @BeforeEach
        void setUp() {

                registry = new SimpleMeterRegistry();

                metrics = new DefaultAgentMicrometerMetrics(
                                registry);

                new MicrometerAgentEvaluationMetrics(
                                registry);

                new MicrometerModelJudgeMetrics(
                                registry);
        }

        @Test
        void shouldKeepExistingMeterNamesStable() {

                Set<String> actualExplicitMeterNames = registry
                                .getMeters()
                                .stream()
                                .filter(
                                                this::isExplicitBusinessMeter)
                                .map(
                                                meter -> meter
                                                                .getId()
                                                                .getName())
                                .collect(
                                                Collectors.toSet());

                assertEquals(
                                BASELINE_METERS,
                                actualExplicitMeterNames);
        }

        @Test
        void shouldKeepExistingMeterTypesStable() {

                assertEquals(
                                Meter.Type.COUNTER,
                                meterType(
                                                "agent.executions"));

                assertEquals(
                                Meter.Type.COUNTER,
                                meterType(
                                                "agent.llm.calls"));

                assertEquals(
                                Meter.Type.COUNTER,
                                meterType(
                                                "agent.tool.calls"));

                assertEquals(
                                Meter.Type.COUNTER,
                                meterType(
                                                "agent.mcp.calls"));

                assertEquals(
                                Meter.Type.COUNTER,
                                meterType(
                                                "agent.cache.hits"));

                assertEquals(
                                Meter.Type.COUNTER,
                                meterType(
                                                "agent.cache.misses"));

                assertEquals(
                                Meter.Type.COUNTER,
                                meterType(
                                                "agent.retries"));

                assertEquals(
                                Meter.Type.COUNTER,
                                meterType(
                                                "agent.no_progress.suppressions"));

                assertEquals(
                                Meter.Type.TIMER,
                                meterType(
                                                "agent.execution.duration"));

                assertEquals(
                                Meter.Type.TIMER,
                                meterType(
                                                "agent.llm.duration"));

                assertEquals(
                                Meter.Type.TIMER,
                                meterType(
                                                "agent.tool.duration"));

                assertEquals(
                                Meter.Type.TIMER,
                                meterType(
                                                "agent.mcp.duration"));

                assertEquals(
                                Meter.Type.DISTRIBUTION_SUMMARY,
                                meterType(
                                                "agent.evidence.original.chars"));

                assertEquals(
                                Meter.Type.DISTRIBUTION_SUMMARY,
                                meterType(
                                                "agent.evidence.compacted.chars"));

                assertEquals(
                                Meter.Type.COUNTER,
                                meterType(
                                                "agent.evaluation.judge.calls"));

                assertEquals(
                                Meter.Type.COUNTER,
                                meterType(
                                                "agent.evaluation.judge.failures"));

                assertEquals(
                                Meter.Type.DISTRIBUTION_SUMMARY,
                                meterType(
                                                "agent.evaluation.judge.score"));

                assertEquals(
                                Meter.Type.TIMER,
                                meterType(
                                                "agent.evaluation.judge.duration"));

                assertEquals(
                                Meter.Type.COUNTER,
                                meterType(
                                                "agent.evaluation.judge.prompt.tokens"));

                assertEquals(
                                Meter.Type.COUNTER,
                                meterType(
                                                "agent.evaluation.judge.completion.tokens"));

                assertEquals(
                                Meter.Type.COUNTER,
                                meterType(
                                                "agent.evaluation.judge.total.tokens"));

                assertEquals(
                                Meter.Type.COUNTER,
                                meterType(
                                                "agent.evaluation.judge.usage.missing"));

                assertEquals(
                                Meter.Type.COUNTER,
                                meterType(
                                                "agent.evaluation.judge.cost.usd"));

                assertEquals(
                                Meter.Type.COUNTER,
                                meterType(
                                                "agent.evaluation.judge.pricing.missing"));
        }

        @Test
        void shouldKeepCounterSemanticsCumulativeAcrossExecutions() {

                metrics.recordExecution(
                                firstSnapshot());

                metrics.recordExecution(
                                secondSnapshot());

                assertCounter(
                                "agent.executions",
                                2.0);

                assertCounter(
                                "agent.llm.calls",
                                17.0);

                assertCounter(
                                "agent.tool.calls",
                                4.0);

                assertCounter(
                                "agent.mcp.calls",
                                1.0);

                assertCounter(
                                "agent.cache.hits",
                                3.0);

                assertCounter(
                                "agent.cache.misses",
                                1.0);

                assertCounter(
                                "agent.retries",
                                1.0);

                assertCounter(
                                "agent.no_progress.suppressions",
                                1.0);
        }

        @Test
        void shouldKeepTimerSemanticsPerAgentExecution() {

                metrics.recordExecution(
                                firstSnapshot());

                metrics.recordExecution(
                                secondSnapshot());

                assertTimer(
                                "agent.execution.duration",
                                2,
                                150.0);

                assertTimer(
                                "agent.llm.duration",
                                2,
                                139.0);

                assertTimer(
                                "agent.tool.duration",
                                2,
                                0.880);

                assertTimer(
                                "agent.mcp.duration",
                                2,
                                0.660);
        }

        @Test
        void shouldKeepEvidenceSummarySemanticsPerAgentExecution() {

                metrics.recordExecution(
                                firstSnapshot());

                metrics.recordExecution(
                                secondSnapshot());

                assertEquals(
                                2,
                                registry
                                                .summary(
                                                                "agent.evidence.original.chars")
                                                .count());

                assertEquals(
                                52_000.0,
                                registry
                                                .summary(
                                                                "agent.evidence.original.chars")
                                                .totalAmount(),
                                0.001);

                assertEquals(
                                2,
                                registry
                                                .summary(
                                                                "agent.evidence.compacted.chars")
                                                .count());

                assertEquals(
                                24_000.0,
                                registry
                                                .summary(
                                                                "agent.evidence.compacted.chars")
                                                .totalAmount(),
                                0.001);
        }

        @Test
        void shouldKeepExplicitMetricDescriptionsNonBlank() {

                registry
                                .getMeters()
                                .stream()
                                .filter(
                                                this::isExplicitBusinessMeter)
                                .forEach(
                                                meter -> {

                                                        String description = meter
                                                                        .getId()
                                                                        .getDescription();

                                                        assertFalse(
                                                                        description == null
                                                                                        || description.isBlank(),
                                                                        () -> "Explicit metric description "
                                                                                        + "must not be blank: "
                                                                                        + meter
                                                                                                        .getId()
                                                                                                        .getName());
                                                });
        }

        @Test
        void shouldNotUseHighCardinalityOrSensitiveTags() {

                metrics.recordExecution(
                                firstSnapshot());

                metrics.recordGuardrailResults(
                                java.util.List.of(
                                                PerformanceGuardrailResult.warn(
                                                                "llmCalls",
                                                                13,
                                                                12,
                                                                "warn")));

                for (Meter meter : registry.getMeters()) {

                        meter.getId()
                                        .getTags()
                                        .forEach(
                                                        tag -> assertFalse(
                                                                        FORBIDDEN_TAG_KEYS.contains(
                                                                                        normalizeTagKey(
                                                                                                        tag.getKey())),
                                                                        () -> "Forbidden metric tag key: "
                                                                                        + tag.getKey()
                                                                                        + ", meter="
                                                                                        + meter.getId().getName()));
                }
        }

        @Test
        void shouldKeepGuardrailTagsLowCardinality() {

                metrics.recordGuardrailResults(
                                java.util.List.of(
                                                PerformanceGuardrailResult.warn(
                                                                "llmCalls",
                                                                13,
                                                                12,
                                                                "warn"),
                                                PerformanceGuardrailResult.critical(
                                                                "totalDurationMs",
                                                                200_000,
                                                                180_000,
                                                                "critical")));

                Set<String> allowedGuardrailTagKeys = Set.of(
                                "level",
                                "metric");

                registry
                                .find(
                                                "agent.performance.guardrail.events")
                                .meters()
                                .forEach(
                                                meter -> {

                                                        Set<String> actualTagKeys = meter
                                                                        .getId()
                                                                        .getTags()
                                                                        .stream()
                                                                        .map(
                                                                                        tag -> tag.getKey())
                                                                        .collect(
                                                                                        Collectors.toSet());

                                                        assertTrue(
                                                                        allowedGuardrailTagKeys.containsAll(
                                                                                        actualTagKeys),
                                                                        () -> "Unexpected guardrail tag keys: "
                                                                                        + actualTagKeys);
                                                });
        }

        @Test
        void shouldRegisterExecutionDurationHistogramForConfiguredSlos() {

                boolean histogramRegistered = registry
                                .getMeters()
                                .stream()
                                .anyMatch(
                                                meter -> "agent.execution.duration.histogram"
                                                                .equals(
                                                                                meter
                                                                                                .getId()
                                                                                                .getName()));

                assertTrue(
                                histogramRegistered,
                                "Execution duration SLOs must register "
                                                + "the Micrometer histogram meter");
        }

        private AgentPerformanceSnapshot firstSnapshot() {

                return new AgentPerformanceSnapshot(
                                120_000,
                                13,
                                119_000,
                                3,
                                680,
                                1,
                                660,
                                2,
                                1,
                                42_000,
                                16_000,
                                1,
                                1);
        }

        private AgentPerformanceSnapshot secondSnapshot() {

                return new AgentPerformanceSnapshot(
                                30_000,
                                4,
                                20_000,
                                1,
                                200,
                                0,
                                0,
                                1,
                                0,
                                10_000,
                                8_000,
                                0,
                                0);
        }

        private Meter.Type meterType(
                        String name) {

                return registry
                                .get(
                                                name)
                                .meter()
                                .getId()
                                .getType();
        }

        private void assertCounter(
                        String name,
                        double expected) {

                assertEquals(
                                expected,
                                registry
                                                .counter(
                                                                name)
                                                .count(),
                                0.001);
        }

        private void assertTimer(
                        String name,
                        long expectedCount,
                        double expectedSeconds) {

                assertEquals(
                                expectedCount,
                                registry
                                                .timer(
                                                                name)
                                                .count());

                assertEquals(
                                expectedSeconds,
                                registry
                                                .timer(
                                                                name)
                                                .totalTime(
                                                                TimeUnit.SECONDS),
                                0.001);
        }

        private String normalizeTagKey(
                        String tagKey) {

                return tagKey
                                .replace(
                                                "-",
                                                "")
                                .replace(
                                                "_",
                                                "")
                                .toLowerCase(
                                                java.util.Locale.ROOT);
        }

        private boolean isExplicitBusinessMeter(
                        Meter meter) {

                return BASELINE_METERS.contains(
                                meter
                                                .getId()
                                                .getName());
        }
}
