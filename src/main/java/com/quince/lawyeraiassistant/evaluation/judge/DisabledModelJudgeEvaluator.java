package com.quince.lawyeraiassistant.evaluation.judge;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.evaluation.judge", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DisabledModelJudgeEvaluator
        implements ModelJudgeEvaluator {

    @Override
    public ModelJudgeResult evaluate(
            ModelJudgeRequest request) {

        return ModelJudgeResult.disabled();
    }
}