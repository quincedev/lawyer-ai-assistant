package com.quince.lawyeraiassistant.agent.runtime.metrics.micrometer;

import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceSnapshot;
import com.quince.lawyeraiassistant.security.runtime.performance.PerformanceGuardrailResult;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

@Component
public class DefaultAgentMicrometerMetrics
        implements AgentMicrometerMetrics {

    private final MeterRegistry meterRegistry;

    private final Counter executions;

    private final Counter llmCalls;

    private final Counter toolCalls;

    private final Counter mcpCalls;

    private final Counter cacheHits;

    private final Counter cacheMisses;

    private final Counter retries;

    private final Counter noProgressSuppressions;

    private final Timer executionTimer;

    private final Timer llmTimer;

    private final Timer toolTimer;

    private final Timer mcpTimer;

    private final DistributionSummary evidenceOriginalChars;

    private final DistributionSummary evidenceCompactedChars;

    public DefaultAgentMicrometerMetrics(
            MeterRegistry meterRegistry) {

        this.meterRegistry = Objects.requireNonNull(
                meterRegistry,
                "MeterRegistry must not be null");

        this.executions = Counter.builder(
                "agent.executions")
                .description(
                        "Total Agent executions")
                .register(
                        meterRegistry);

        this.llmCalls = Counter.builder(
                "agent.llm.calls")
                .description(
                        "Total LLM calls across Agent executions")
                .register(
                        meterRegistry);

        this.toolCalls = Counter.builder(
                "agent.tool.calls")
                .description(
                        "Total Tool calls")
                .register(
                        meterRegistry);

        this.mcpCalls = Counter.builder(
                "agent.mcp.calls")
                .description(
                        "Total MCP calls")
                .register(
                        meterRegistry);

        this.cacheHits = Counter.builder(
                "agent.cache.hits")
                .description(
                        "Total Agent Tool cache hits")
                .register(
                        meterRegistry);

        this.cacheMisses = Counter.builder(
                "agent.cache.misses")
                .description(
                        "Total Agent Tool cache misses")
                .register(
                        meterRegistry);

        this.retries = Counter.builder(
                "agent.retries")
                .description(
                        "Total Agent retries")
                .register(
                        meterRegistry);

        this.noProgressSuppressions = Counter.builder(
                "agent.no_progress.suppressions")
                .description(
                        "Total no-progress retry suppressions")
                .register(
                        meterRegistry);

        this.executionTimer = Timer.builder(
                "agent.execution.duration")
                .description(
                        "Agent execution duration")
                .publishPercentileHistogram()
                .serviceLevelObjectives(
                        Duration.ofSeconds(30),
                        Duration.ofSeconds(60),
                        Duration.ofSeconds(120),
                        Duration.ofSeconds(180))
                .register(
                        meterRegistry);

        this.llmTimer = Timer.builder(
                "agent.llm.duration")
                .description(
                        "Aggregated LLM duration per Agent execution")
                .publishPercentileHistogram()
                .register(
                        meterRegistry);

        this.toolTimer = Timer.builder(
                "agent.tool.duration")
                .description(
                        "Aggregated Tool duration per Agent execution")
                .publishPercentileHistogram()
                .register(
                        meterRegistry);

        this.mcpTimer = Timer.builder(
                "agent.mcp.duration")
                .description(
                        "Aggregated MCP duration per Agent execution")
                .publishPercentileHistogram()
                .register(
                        meterRegistry);

        this.evidenceOriginalChars = DistributionSummary.builder(
                "agent.evidence.original.chars")
                .description(
                        "Original evidence size before compaction")
                .register(
                        meterRegistry);

        this.evidenceCompactedChars = DistributionSummary.builder(
                "agent.evidence.compacted.chars")
                .description(
                        "Evidence size after compaction")
                .register(
                        meterRegistry);
    }

    @Override
    public void recordExecution(
            AgentPerformanceSnapshot snapshot) {

        Objects.requireNonNull(
                snapshot,
                "AgentPerformanceSnapshot must not be null");

        executions.increment();

        llmCalls.increment(
                snapshot.llmCalls());

        toolCalls.increment(
                snapshot.toolCalls());

        mcpCalls.increment(
                snapshot.mcpCalls());

        cacheHits.increment(
                snapshot.cacheHits());

        cacheMisses.increment(
                snapshot.cacheMisses());

        retries.increment(
                snapshot.retries());

        noProgressSuppressions.increment(
                snapshot.noProgressSuppressions());

        executionTimer.record(
                Duration.ofMillis(
                        snapshot.totalDurationMs()));

        llmTimer.record(
                Duration.ofMillis(
                        snapshot.llmDurationMs()));

        toolTimer.record(
                Duration.ofMillis(
                        snapshot.toolDurationMs()));

        mcpTimer.record(
                Duration.ofMillis(
                        snapshot.mcpDurationMs()));

        evidenceOriginalChars.record(
                snapshot.evidenceOriginalChars());

        evidenceCompactedChars.record(
                snapshot.evidenceCompactedChars());
    }

    @Override
    public void recordGuardrailResults(
            List<PerformanceGuardrailResult> results) {

        if (results == null
                || results.isEmpty()) {

            return;
        }

        for (PerformanceGuardrailResult result : results) {

            Counter.builder(
                    "agent.performance.guardrail.events")
                    .description(
                            "Agent performance guardrail events")
                    .tag(
                            "level",
                            result.level().name())
                    .tag(
                            "metric",
                            result.metric())
                    .register(
                            meterRegistry)
                    .increment();
        }
    }
}