package com.quince.lawyeraiassistant.evaluation;

import com.quince.lawyeraiassistant.evaluation.model.AgentEvaluationCandidate;
import com.quince.lawyeraiassistant.evaluation.model.AgentEvaluationCase;
import com.quince.lawyeraiassistant.evaluation.model.AgentEvaluationResult;

public interface AgentQualityEvaluator {

    AgentEvaluationResult evaluate(
            AgentEvaluationCase evaluationCase,
            AgentEvaluationCandidate candidate);
}