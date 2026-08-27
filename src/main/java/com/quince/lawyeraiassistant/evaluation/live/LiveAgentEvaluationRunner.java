package com.quince.lawyeraiassistant.evaluation.live;

public interface LiveAgentEvaluationRunner {

    LiveEvaluationReport runAll();

    LiveEvaluationReport runCase(
            String caseId);
}