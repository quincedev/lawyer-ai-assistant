package com.quince.lawyeraiassistant.agent.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReflectionResultTest {

    @Test
    void shouldCreateContinueResult() {

        ReflectionResult result = ReflectionResult.of(
                ReflectionDecision.CONTINUE,
                "当前任务已经获得足够结果");

        assertEquals(
                ReflectionDecision.CONTINUE,
                result.getDecision());

        assertTrue(
                result.shouldContinue());

        assertFalse(
                result.shouldRetry());

        assertFalse(
                result.shouldReplan());

        assertFalse(
                result.shouldFinish());
    }

    @Test
    void shouldCreateRetryResult() {

        ReflectionResult result = ReflectionResult.of(
                ReflectionDecision.RETRY,
                "当前结果不足");

        assertTrue(
                result.shouldRetry());
    }

    @Test
    void shouldCreateReplanResult() {

        ReflectionResult result = ReflectionResult.of(
                ReflectionDecision.REPLAN,
                "原计划已经不再适用");

        assertTrue(
                result.shouldReplan());
    }

    @Test
    void shouldCreateFinishResult() {

        ReflectionResult result = ReflectionResult.of(
                ReflectionDecision.FINISH,
                "已有信息足以完成用户目标");

        assertTrue(
                result.shouldFinish());
    }

    @Test
    void shouldNormalizeSummary() {

        ReflectionResult result = ReflectionResult.of(
                ReflectionDecision.CONTINUE,
                "  当前任务完成  ");

        assertEquals(
                "当前任务完成",
                result.getSummary());
    }

    @Test
    void shouldRejectNullDecision() {

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> ReflectionResult.of(
                        null,
                        "summary"));

        assertEquals(
                "ReflectionDecision must not be null",
                exception.getMessage());
    }

    @Test
    void shouldRejectNullSummary() {

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> ReflectionResult.of(
                        ReflectionDecision.CONTINUE,
                        null));

        assertEquals(
                "Reflection summary must not be null",
                exception.getMessage());
    }

    @Test
    void shouldRejectBlankSummary() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ReflectionResult.of(
                        ReflectionDecision.CONTINUE,
                        "   "));

        assertEquals(
                "Reflection summary must not be blank",
                exception.getMessage());
    }
}