package com.quince.lawyeraiassistant.evaluation;

import com.quince.lawyeraiassistant.evaluation.metrics.AgentEvaluationMetrics;
import com.quince.lawyeraiassistant.evaluation.model.AgentEvaluationCandidate;
import com.quince.lawyeraiassistant.evaluation.model.AgentEvaluationCase;
import com.quince.lawyeraiassistant.evaluation.model.AgentEvaluationResult;

import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class DefaultAgentEvaluationService
        implements AgentEvaluationService {

    private final AgentQualityEvaluator evaluator;

    private final AgentEvaluationMetrics evaluationMetrics;

    public DefaultAgentEvaluationService(
            AgentQualityEvaluator evaluator,
            AgentEvaluationMetrics evaluationMetrics) {

        this.evaluator = Objects.requireNonNull(
                evaluator,
                "evaluator must not be null");

        this.evaluationMetrics = Objects.requireNonNull(
                evaluationMetrics,
                "evaluationMetrics must not be null");
    }

    @Override
    public AgentEvaluationResult evaluate(
            AgentEvaluationCase evaluationCase,
            AgentEvaluationCandidate candidate) {

        AgentEvaluationResult result = evaluator.evaluate(
                evaluationCase,
                candidate);

        evaluationMetrics.record(
                result);

        return result;
    }
}