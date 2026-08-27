package com.quince.lawyeraiassistant.evaluation.model;

import com.quince.lawyeraiassistant.agent.model.AgentStatus;
import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceSnapshot;

import java.util.Objects;

public record AgentEvaluationCandidate(
        String answer,
        AgentStatus status,
        AgentPerformanceSnapshot performance) {

    public AgentEvaluationCandidate {

        answer = answer == null
                ? ""
                : answer.trim();

        Objects.requireNonNull(
                status,
                "status must not be null");

        Objects.requireNonNull(
                performance,
                "performance must not be null");
    }
}