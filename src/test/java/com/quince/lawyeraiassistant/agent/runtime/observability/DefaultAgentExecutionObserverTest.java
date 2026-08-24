package com.quince.lawyeraiassistant.agent.runtime.observability;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

        observer = new DefaultAgentExecutionObserver(
                performanceContext,
                performanceGuardrailService,
                micrometerMetrics,
                performanceSnapshotRecorder);
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
}
