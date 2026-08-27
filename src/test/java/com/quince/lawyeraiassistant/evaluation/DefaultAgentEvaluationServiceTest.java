package com.quince.lawyeraiassistant.evaluation;

import com.quince.lawyeraiassistant.agent.model.AgentStatus;
import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceSnapshot;
import com.quince.lawyeraiassistant.evaluation.metrics.AgentEvaluationMetrics;
import com.quince.lawyeraiassistant.evaluation.model.AgentEvaluationCandidate;
import com.quince.lawyeraiassistant.evaluation.model.AgentEvaluationCase;
import com.quince.lawyeraiassistant.evaluation.model.AgentEvaluationResult;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultAgentEvaluationServiceTest {

    @Test
    void shouldEvaluateAndRecordMetrics() {

        AgentQualityEvaluator evaluator = mock(AgentQualityEvaluator.class);

        AgentEvaluationMetrics metrics = mock(AgentEvaluationMetrics.class);

        AgentEvaluationCase evaluationCase = evaluationCase();

        AgentEvaluationCandidate candidate = candidate();

        AgentEvaluationResult expected = result();

        when(
                evaluator.evaluate(
                        evaluationCase,
                        candidate))
                .thenReturn(expected);

        DefaultAgentEvaluationService service = new DefaultAgentEvaluationService(
                evaluator,
                metrics);

        AgentEvaluationResult actual = service.evaluate(
                evaluationCase,
                candidate);

        assertSame(
                expected,
                actual);

        verify(evaluator).evaluate(
                evaluationCase,
                candidate);

        verify(metrics).record(
                expected);
    }

    private AgentEvaluationCase evaluationCase() {

        return new AgentEvaluationCase(
                "test-case",
                "labor-law",
                "测试问题",
                List.of("赔偿金"),
                List.of(),
                10,
                1.0D,
                75.0D,
                new BigDecimal("0.05"));
    }

    private AgentEvaluationCandidate candidate() {

        return new AgentEvaluationCandidate(
                "应当支付赔偿金。",
                AgentStatus.FINISHED,
                new AgentPerformanceSnapshot(
                        1_000,
                        1,
                        900,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0));
    }

    private AgentEvaluationResult result() {

        return new AgentEvaluationResult(
                "test-case",
                100.0D,
                75.0D,
                true,
                1.0D,
                List.of("赔偿金"),
                List.of(),
                List.of(),
                List.of(),
                new BigDecimal("0.001"),
                new BigDecimal("0.05"),
                true);
    }
}