package com.quince.lawyeraiassistant.evaluation.judge;

public interface ModelJudgeEvaluator {

    ModelJudgeResult evaluate(
            ModelJudgeRequest request);
}