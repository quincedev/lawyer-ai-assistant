package com.quince.lawyeraiassistant.evaluation.judge.metrics;

import com.quince.lawyeraiassistant.evaluation.judge.ModelJudgeResult;

public interface ModelJudgeMetrics {

    void record(
            ModelJudgeResult result);
}
