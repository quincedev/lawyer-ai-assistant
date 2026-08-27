package com.quince.lawyeraiassistant.agent.runtime.observability;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.runtime.cost.AgentCostProperties;
import com.quince.lawyeraiassistant.agent.runtime.cost.AgentExecutionCostAttributionRecorder;
import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceContext;
import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceMetrics;
import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceSnapshot;
import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceSnapshotRecorder;
import com.quince.lawyeraiassistant.agent.runtime.metrics.micrometer.AgentMicrometerMetrics;
import com.quince.lawyeraiassistant.security.runtime.performance.PerformanceGuardrailResult;
import com.quince.lawyeraiassistant.security.runtime.performance.PerformanceGuardrailService;

class DefaultAgentExecutionObserverTest {

    private AgentPerformanceContext performanceContext;

    private PerformanceGuardrailService performanceGuardrailService;

    private AgentMicrometerMetrics micrometerMetrics;

    private AgentPerformanceSnapshotRecorder performanceSnapshotRecorder;

    private AgentExecutionCostAttributionRecorder costAttributionRecorder;

    private AgentCostProperties costProperties;

    private DefaultAgentExecutionObserver observer;

    @BeforeEach
    void setUp() {

        performanceContext = mock(
                AgentPerformanceContext.class);

        performanceGuardrailService = mock(
                PerformanceGuardrailService.class);

        micrometerMetrics = mock(
                AgentMicrometerMetrics.class);

        performanceSnapshotRecorder = mock(
                AgentPerformanceSnapshotRecorder.class);

        costAttributionRecorder = mock(
                AgentExecutionCostAttributionRecorder.class);

        costProperties = new AgentCostProperties();

        when(performanceGuardrailService.evaluate(any()))
                .thenReturn(List.of());

        observer = new DefaultAgentExecutionObserver(
                performanceContext,
                performanceGuardrailService,
                micrometerMetrics,
                performanceSnapshotRecorder,
                costAttributionRecorder,
                costProperties);
    }

    @Test
    void shouldStartPerformanceContext() {

        AgentPerformanceMetrics metrics = mock(
                AgentPerformanceMetrics.class);

        when(
                performanceContext.start())
                .thenReturn(
                        metrics);

        AgentExecutionObservation observation = observer.start();

        assertSame(
                metrics,
                observation.performanceMetrics());

        verify(
                performanceContext)
                .start();
    }

    @Test
    void shouldCreateDifferentExecutionIdForEveryStart() {

        when(performanceContext.start())
                .thenReturn(mock(AgentPerformanceMetrics.class));

        AgentExecutionObservation first = observer.start();
        AgentExecutionObservation second = observer.start();

        assertNotEquals(first.executionId(), second.executionId());
    }

    @Test
    void shouldRecordRetryOnCurrentMetrics() {

        AgentPerformanceMetrics metrics = mock(
                AgentPerformanceMetrics.class);

        when(
                performanceContext.current())
                .thenReturn(
                        Optional.of(
                                metrics));

        observer.recordRetry();

        verify(
                metrics)
                .recordRetry();
    }

    @Test
    void shouldRecordNoProgressSuppressionOnCurrentMetrics() {

        AgentPerformanceMetrics metrics = mock(
                AgentPerformanceMetrics.class);

        when(
                performanceContext.current())
                .thenReturn(
                        Optional.of(
                                metrics));

        observer.recordNoProgressSuppression();

        verify(
                metrics)
                .recordNoProgressSuppression();
    }

    @Test
    void shouldCompletePerformanceObservationInOrder() {

        AgentPerformanceMetrics metrics = mock(
                AgentPerformanceMetrics.class);

        AgentPerformanceSnapshot snapshot = mock(
                AgentPerformanceSnapshot.class);

        AgentExecutionObservation observation = new AgentExecutionObservation(
                "execution-test-001",
                System.nanoTime(),
                metrics);

        List<PerformanceGuardrailResult> guardrailResults = List.of(
                PerformanceGuardrailResult.ok(
                        "llmCalls",
                        1));

        when(
                metrics.snapshot())
                .thenReturn(
                        snapshot);

        when(
                performanceGuardrailService.evaluate(
                        snapshot))
                .thenReturn(
                        guardrailResults);

        observer.complete(
                observation,
                mock(AgentContext.class),
                null,
                AgentExecutionStatus.SUCCESS);

        InOrder inOrder = inOrder(
                metrics,
                performanceSnapshotRecorder,
                micrometerMetrics,
                performanceGuardrailService,
                performanceContext);

        inOrder.verify(
                metrics)
                .snapshot();

        inOrder.verify(
                performanceSnapshotRecorder)
                .record(
                        snapshot);

        inOrder.verify(
                micrometerMetrics)
                .recordExecution(
                        snapshot);

        inOrder.verify(
                performanceGuardrailService)
                .evaluate(
                        snapshot);

        inOrder.verify(
                micrometerMetrics)
                .recordGuardrailResults(
                        guardrailResults);

        inOrder.verify(
                performanceContext)
                .clear();
    }

    @Test
    void shouldClearPerformanceContextWhenGuardrailEvaluationThrows() {

        AgentPerformanceMetrics metrics = mock(
                AgentPerformanceMetrics.class);

        AgentPerformanceSnapshot snapshot = mock(
                AgentPerformanceSnapshot.class);

        AgentExecutionObservation observation = new AgentExecutionObservation(
                "execution-test-001",
                System.nanoTime(),
                metrics);

        RuntimeException guardrailFailure = new RuntimeException(
                "guardrail failure");

        when(
                metrics.snapshot())
                .thenReturn(
                        snapshot);

        when(
                performanceGuardrailService.evaluate(
                        snapshot))
                .thenThrow(
                        guardrailFailure);

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> observer.complete(
                        observation,
                        mock(AgentContext.class),
                        null,
                        AgentExecutionStatus.FAILED));

        assertSame(
                guardrailFailure,
                thrown);

        verify(
                performanceContext)
                .clear();
    }

    @Test
    void shouldSaveAttributionOnComplete() {

        AgentPerformanceSnapshot snapshot = complete(
                AgentExecutionStatus.SUCCESS,
                snapshot(2, 0, 2, 0, 750_000));

        verify(costAttributionRecorder).record(argThat(attribution ->
                attribution.executionId().equals("execution-test-001")
                        && attribution.executionStatus() == AgentExecutionStatus.SUCCESS
                        && attribution.llmCalls() == snapshot.llmCalls()));
    }

    @Test
    void shouldRecordCostForSuccessfulExecution() {

        complete(
                AgentExecutionStatus.SUCCESS,
                snapshot(2, 0, 2, 0, 750_000));

        verify(costAttributionRecorder).record(argThat(attribution ->
                attribution.costMicros() == 750_000
                        && attribution.fullyAttributed()));
    }

    @Test
    void shouldRecordAvailableCostForFailedExecution() {

        complete(
                AgentExecutionStatus.FAILED,
                snapshot(2, 1, 1, 0, 250_000));

        verify(costAttributionRecorder).record(argThat(attribution ->
                attribution.executionStatus() == AgentExecutionStatus.FAILED
                        && attribution.costMicros() == 250_000
                        && !attribution.fullyAttributed()));
    }

    @Test
    void shouldClearPerformanceContextAfterComplete() {

        complete(
                AgentExecutionStatus.SUCCESS,
                snapshot(1, 0, 1, 0, 10));

        verify(performanceContext).clear();
    }

    private AgentPerformanceSnapshot complete(
            AgentExecutionStatus status,
            AgentPerformanceSnapshot snapshot) {

        AgentPerformanceMetrics metrics = mock(AgentPerformanceMetrics.class);
        when(metrics.snapshot()).thenReturn(snapshot);

        observer.complete(
                new AgentExecutionObservation(
                        "execution-test-001",
                        System.nanoTime(),
                        metrics),
                AgentContext.from("test goal"),
                AgentContext.from("test result"),
                status);

        return snapshot;
    }

    private AgentPerformanceSnapshot snapshot(
            int llmCalls,
            int usageMissingCalls,
            long pricedLlmCalls,
            long pricingMissingCalls,
            long costMicros) {

        return new AgentPerformanceSnapshot(
                100, llmCalls, 80,
                100, 20, 120, usageMissingCalls,
                1, 10, 0, 0, 0, 0,
                0, 0, 0, 0,
                costMicros, pricedLlmCalls, pricingMissingCalls);
    }
}
