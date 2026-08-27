package com.quince.lawyeraiassistant.evaluation.metrics;

import com.quince.lawyeraiassistant.evaluation.model.AgentEvaluationResult;

public interface AgentEvaluationMetrics {

    void record(
            AgentEvaluationResult result);
}