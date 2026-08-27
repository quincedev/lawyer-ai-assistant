package com.quince.lawyeraiassistant.agent.runtime.observability;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentStatus;
import com.quince.lawyeraiassistant.agent.runtime.cost.AgentCostProperties;
import com.quince.lawyeraiassistant.agent.runtime.cost.AgentExecutionCostAttribution;
import com.quince.lawyeraiassistant.agent.runtime.cost.AgentExecutionCostAttributionRecorder;
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

        private final AgentExecutionCostAttributionRecorder costAttributionRecorder;

        private final AgentCostProperties costProperties;

        public DefaultAgentExecutionObserver(
                        AgentPerformanceContext performanceContext,
                        PerformanceGuardrailService performanceGuardrailService,
                        AgentMicrometerMetrics micrometerMetrics,
                        AgentPerformanceSnapshotRecorder performanceSnapshotRecorder,
                        AgentExecutionCostAttributionRecorder costAttributionRecorder,
                        AgentCostProperties costProperties) {

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

                this.costAttributionRecorder = Objects.requireNonNull(
                                costAttributionRecorder,
                                "costAttributionRecorder must not be null");

                this.costProperties = Objects.requireNonNull(
                                costProperties,
                                "costProperties must not be null");
        }

        @Override
        public AgentExecutionObservation start() {

                AgentPerformanceMetrics metrics = performanceContext.start();

                return new AgentExecutionObservation(
                                UUID.randomUUID().toString(),
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

                        /*
                         * 必须先创建 Snapshot，
                         * 后续 Recorder、Cost Attribution、
                         * Micrometer 和 Guardrail 共用同一份结果。
                         */
                        AgentPerformanceSnapshot snapshot = observation.performanceMetrics()
                                        .snapshot();

                        performanceSnapshotRecorder.record(
                                        snapshot);

                        AgentExecutionCostAttribution attribution = createCostAttribution(
                                        observation,
                                        initialContext,
                                        result,
                                        executionStatus,
                                        snapshot);

                        costAttributionRecorder.record(
                                        attribution);

                        logPerformanceSummary(
                                        snapshot);

                        logCostAttribution(
                                        attribution);

                        micrometerMetrics.recordExecution(
                                        snapshot);

                        List<PerformanceGuardrailResult> guardrailResults = performanceGuardrailService.evaluate(
                                        snapshot);

                        guardrailResults.forEach(
                                        this::logPerformanceGuardrailResult);

                        micrometerMetrics.recordGuardrailResults(
                                        guardrailResults);

                } finally {

                        /*
                         * 无论 Snapshot、日志、Micrometer 或
                         * Guardrail 是否异常，都必须清理线程上下文。
                         */
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
                                                + "noProgressSuppressions={}, "
                                                + "promptTokens={}, "
                                                + "completionTokens={}, "
                                                + "totalTokens={}, "
                                                + "usageMissingCalls={}, "
                                                + "tokenUsageCoverageRatio={}, "
                                                + "pricedLlmCalls={}, "
                                                + "pricingMissingCalls={}, "
                                                + "pricingCoverageRatio={}, "
                                                + "llmCostMicros={}, "
                                                + "llmCostUsd={}",
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
                                snapshot.noProgressSuppressions(),
                                snapshot.promptTokens(),
                                snapshot.completionTokens(),
                                snapshot.totalTokens(),
                                snapshot.usageMissingCalls(),
                                snapshot.tokenUsageCoverageRatio(),
                                snapshot.pricedLlmCalls(),
                                snapshot.pricingMissingCalls(),
                                snapshot.pricingCoverageRatio(),
                                snapshot.llmCostMicros(),
                                snapshot.llmCostUsd());
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

        private AgentExecutionCostAttribution createCostAttribution(
                        AgentExecutionObservation observation,
                        AgentContext initialContext,
                        AgentContext result,
                        AgentExecutionStatus executionStatus,
                        AgentPerformanceSnapshot snapshot) {

                String tenantId = initialContext.getTenantContext() == null
                                ? "unknown"
                                : initialContext.getTenantContext()
                                                .tenantId();

                AgentStatus agentStatus = result == null
                                ? null
                                : result.getStatus();

                return AgentExecutionCostAttribution.from(
                                observation.executionId(),
                                tenantId,
                                executionStatus,
                                agentStatus,
                                costProperties.getCurrency(),
                                snapshot);
        }

        private void logCostAttribution(
                        AgentExecutionCostAttribution attribution) {

                log.info(
                                "Agent execution cost attributed. "
                                                + "executionId={}, "
                                                + "tenantId={}, "
                                                + "executionStatus={}, "
                                                + "agentStatus={}, "
                                                + "llmCalls={}, "
                                                + "promptTokens={}, "
                                                + "completionTokens={}, "
                                                + "totalTokens={}, "
                                                + "usageMissingCalls={}, "
                                                + "pricedLlmCalls={}, "
                                                + "pricingMissingCalls={}, "
                                                + "currency={}, "
                                                + "costMicros={}, "
                                                + "cost={}, "
                                                + "fullyAttributed={}",
                                attribution.executionId(),
                                attribution.tenantId(),
                                attribution.executionStatus(),
                                attribution.agentStatus(),
                                attribution.llmCalls(),
                                attribution.promptTokens(),
                                attribution.completionTokens(),
                                attribution.totalTokens(),
                                attribution.usageMissingCalls(),
                                attribution.pricedLlmCalls(),
                                attribution.pricingMissingCalls(),
                                attribution.currency(),
                                attribution.costMicros(),
                                attribution.cost(),
                                attribution.fullyAttributed());
        }
}