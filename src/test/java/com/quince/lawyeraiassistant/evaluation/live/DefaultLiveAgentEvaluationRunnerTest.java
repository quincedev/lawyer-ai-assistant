package com.quince.lawyeraiassistant.evaluation.live;

import com.quince.lawyeraiassistant.agent.application.AgentApplicationService;
import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceSnapshotRecorder;
import com.quince.lawyeraiassistant.common.exception.ResourceNotFoundException;
import com.quince.lawyeraiassistant.evaluation.AgentEvaluationService;
import com.quince.lawyeraiassistant.evaluation.judge.ModelJudgeEvaluator;
import com.quince.lawyeraiassistant.evaluation.judge.ModelJudgeProperties;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultLiveAgentEvaluationRunnerTest {

    @Test
    void shouldRejectUnknownCaseAsNotFound() {

        AgentEvaluationDataset dataset = mock(AgentEvaluationDataset.class);

        when(dataset.findById("unknown-case-id"))
                .thenReturn(Optional.empty());

        DefaultLiveAgentEvaluationRunner runner = new DefaultLiveAgentEvaluationRunner(
                dataset,
                mock(AgentApplicationService.class),
                mock(AgentEvaluationService.class),
                mock(AgentPerformanceSnapshotRecorder.class),
                new LiveEvaluationProperties(),
                mock(ModelJudgeEvaluator.class),
                new ModelJudgeProperties());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> runner.runCase("unknown-case-id"));

        assertEquals(
                "Evaluation case not found: unknown-case-id",
                exception.getMessage());
    }
}
