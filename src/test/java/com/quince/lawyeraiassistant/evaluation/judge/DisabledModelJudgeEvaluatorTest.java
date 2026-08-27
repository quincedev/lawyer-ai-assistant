package com.quince.lawyeraiassistant.evaluation.judge;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisabledModelJudgeEvaluatorTest {

    private final DisabledModelJudgeEvaluator evaluator =
            new DisabledModelJudgeEvaluator();

    @Test
    void shouldReturnDisabledResultWithoutCallingModel() {
        ModelJudgeResult result = evaluator.evaluate(
                new ModelJudgeRequest(
                        "case-001",
                        "labor-law",
                        "测试问题",
                        List.of("法律概念"),
                        List.of("禁止声明"),
                        "测试答案"));

        assertEquals(ModelJudgeStatus.DISABLED, result.status());
        assertTrue(result.passed());
        assertEquals(0, result.totalTokens());
        assertEquals(0, result.costUsd().signum());
    }
}
