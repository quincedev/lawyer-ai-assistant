package com.quince.lawyeraiassistant.evaluation.judge.metrics;

import com.quince.lawyeraiassistant.evaluation.judge.ModelJudgeResult;
import com.quince.lawyeraiassistant.evaluation.judge.ModelJudgeStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MicrometerModelJudgeMetricsTest {

    private SimpleMeterRegistry registry;
    private MicrometerModelJudgeMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new MicrometerModelJudgeMetrics(registry);
    }

    @Test
    void shouldRecordCompletedJudgeEvaluation() {
        metrics.record(completedResult());
        assertCounter("agent.evaluation.judge.calls", 1.0D, "result", "pass");
        assertEquals(92.0D, registry.get("agent.evaluation.judge.score")
                .summary().totalAmount(), 0.000001D);
        assertCounter("agent.evaluation.judge.prompt.tokens", 1200.0D);
        assertCounter("agent.evaluation.judge.completion.tokens", 300.0D);
        assertCounter("agent.evaluation.judge.total.tokens", 1500.0D);
        assertCounter("agent.evaluation.judge.cost.usd", 0.0015D);
    }

    @Test
    void shouldRecordTechnicalFailureSeparately() {
        metrics.record(ModelJudgeResult.failed("IllegalStateException", true, 25));
        assertCounter("agent.evaluation.judge.failures", 1.0D);
        assertEquals(25L, registry.get("agent.evaluation.judge.duration")
                .timer().totalTime(java.util.concurrent.TimeUnit.MILLISECONDS), 0.000001D);
    }

    @Test
    void shouldIgnoreDisabledAndSkippedResults() {
        metrics.record(ModelJudgeResult.disabled());
        metrics.record(ModelJudgeResult.skipped());
        assertCounter("agent.evaluation.judge.failures", 0.0D);
    }

    private void assertCounter(String name, double expected, String... tags) {
        assertEquals(expected, registry.get(name).tags(tags).counter().count(), 0.000001D);
    }

    private ModelJudgeResult completedResult() {
        return new ModelJudgeResult(
                ModelJudgeStatus.COMPLETED, true, 92.0D, 95.0D, 90.0D,
                94.0D, 89.0D, List.of("Legal conclusion is correct"),
                List.of(), "deepseek-v4-flash", 1500, 1200, 300, 1500,
                true, true, new BigDecimal("0.0015"), "");
    }
}
