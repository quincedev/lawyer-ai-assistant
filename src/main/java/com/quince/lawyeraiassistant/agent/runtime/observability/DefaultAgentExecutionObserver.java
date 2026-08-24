package com.quince.lawyeraiassistant.agent.runtime.observability;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentStatus;
import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceContext;
import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceMetrics;
import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceSnapshot;
import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceSnapshotRecorder;
import com.quince.lawyeraiassistant.agent.runtime.metrics.micrometer.AgentMicrometerMetrics;
import com.quince.lawyeraiassistant.security.runtime.performance.PerformanceGuardrailResult;
import com.quince.lawyeraiassistant.security.runtime.performance.PerformanceGuardrailService;

@Component
public final class DefaultAgentExecutionObserver
                implements AgentExecutionObserver {

        private static final Logger log = LoggerFactory.getLogger(
                        DefaultAgentExecutionObserver.class);

        private final AgentPerformanceContext performanceContext;

        private final PerformanceGuardrailService performanceGuardrailService;

        private final AgentMicrometerMetrics micrometerMetrics;

        private final AgentPerformanceSnapshotRecorder performanceSnapshotRecorder;

        public DefaultAgentExecutionObserver(
                        AgentPerformanceContext performanceContext,
                        PerformanceGuardrailService performanceGuardrailService,
                        AgentMicrometerMetrics micrometerMetrics,
                        AgentPerformanceSnapshotRecorder performanceSnapshotRecorder) {

                this.performanceContext = Objects.requireNonNull(
                                performanceContext,
                                "performanceContext must not be null");

                this.performanceGuardrailService = Objects.requireNonNull(
                                performanceGuardrailService,
                                "performanceGuardrailService must not be null");

                this.micrometerMetrics = Objects.requireNonNull(
                                micrometerMetrics,
                                "micrometerMetrics must not be null");

                this.performanceSnapshotRecorder = Objects.requireNonNull(
                                performanceSnapshotRecorder,
                                "performanceSnapshotRecorder must not be null");
        }

        @Override
        public AgentExecutionObservation start() {

                AgentPerformanceMetrics metrics = performanceContext.start();

                return new AgentExecutionObservation(
                                System.nanoTime(),
                                metrics);
        }

        @Override
        public void complete(
                        AgentExecutionObservation observation,
                        AgentContext initialContext,
                        AgentContext result,
                        AgentExecutionStatus executionStatus) {

                Objects.requireNonNull(
                                observation,
                                "observation must not be null");

                Objects.requireNonNull(
                                initialContext,
                                "initialContext must not be null");

                Objects.requireNonNull(
                                executionStatus,
                                "executionStatus must not be null");

                long durationMs = TimeUnit.NANOSECONDS.toMillis(
                                System.nanoTime()
                                                - observation.startedAtNanos());

                logAgentExecutionFinished(
                                initialContext,
                                result,
                                executionStatus,
                                durationMs);

                try {

                        AgentPerformanceSnapshot snapshot = observation.performanceMetrics()
                                        .snapshot();

                        performanceSnapshotRecorder.record(
                                        snapshot);

                        logPerformanceSummary(
                                        snapshot);

                        micrometerMetrics.recordExecution(
                                        snapshot);

                        List<PerformanceGuardrailResult> guardrailResults = performanceGuardrailService.evaluate(
                                        snapshot);

                        guardrailResults.forEach(
                                        this::logPerformanceGuardrailResult);

                        micrometerMetrics.recordGuardrailResults(
                                        guardrailResults);

                } finally {

                        performanceContext.clear();
                }
        }

        private void logAgentExecutionFinished(
                        AgentContext initialContext,
                        AgentContext result,
                        AgentExecutionStatus executionStatus,
                        long durationMs) {

                String tenantId = initialContext.getTenantContext() == null
                                ? "unknown"
                                : initialContext.getTenantContext()
                                                .tenantId();

                AgentStatus agentStatus = result == null
                                ? null
                                : result.getStatus();

                log.info(
                                "Agent execution finished. tenantId={}, executionStatus={}, agentStatus={}, durationMs={}",
                                tenantId,
                                executionStatus,
                                agentStatus,
                                durationMs);
        }

        private void logPerformanceSummary(
                        AgentPerformanceSnapshot snapshot) {

                log.info(
                                "Agent performance summary. "
                                                + "totalDurationMs={}, "
                                                + "llmCalls={}, "
                                                + "llmDurationMs={}, "
                                                + "llmRatio={}, "
                                                + "toolCalls={}, "
                                                + "toolDurationMs={}, "
                                                + "mcpCalls={}, "
                                                + "mcpDurationMs={}, "
                                                + "cacheHits={}, "
                                                + "cacheMisses={}, "
                                                + "cacheHitRatio={}, "
                                                + "evidenceOriginalChars={}, "
                                                + "evidenceCompactedChars={}, "
                                                + "evidenceReductionRatio={}, "
                                                + "retries={}, "
                                                + "noProgressSuppressions={}",
                                snapshot.totalDurationMs(),
                                snapshot.llmCalls(),
                                snapshot.llmDurationMs(),
                                snapshot.llmDurationRatio(),
                                snapshot.toolCalls(),
                                snapshot.toolDurationMs(),
                                snapshot.mcpCalls(),
                                snapshot.mcpDurationMs(),
                                snapshot.cacheHits(),
                                snapshot.cacheMisses(),
                                snapshot.cacheHitRatio(),
                                snapshot.evidenceOriginalChars(),
                                snapshot.evidenceCompactedChars(),
                                snapshot.evidenceReductionRatio(),
                                snapshot.retries(),
                                snapshot.noProgressSuppressions());
        }

        private void logPerformanceGuardrailResult(
                        PerformanceGuardrailResult result) {

                if (result.isCritical()) {

                        log.error(
                                        "Agent performance guardrail critical. metric={}, actual={}, threshold={}, reason={}",
                                        result.metric(),
                                        result.actual(),
                                        result.threshold(),
                                        result.reason());

                } else if (result.isWarn()) {

                        log.warn(
                                        "Agent performance guardrail warning. metric={}, actual={}, threshold={}, reason={}",
                                        result.metric(),
                                        result.actual(),
                                        result.threshold(),
                                        result.reason());
                }
        }

        @Override
        public void recordRetry() {

                performanceContext
                                .current()
                                .ifPresent(
                                                AgentPerformanceMetrics::recordRetry);
        }

        @Override
        public void recordNoProgressSuppression() {

                performanceContext
                                .current()
                                .ifPresent(
                                                AgentPerformanceMetrics::recordNoProgressSuppression);
        }
}