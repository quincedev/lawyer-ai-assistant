package com.quince.lawyeraiassistant.agent.prompt.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReplanningPromptContextTest {

    @Test
    void shouldCreateReplanningPromptContext() {

        ReplanningPromptContext context = new ReplanningPromptContext(
                "分析劳动合同",
                "用户希望分析合同风险",
                "task-1 | COMPLETED",
                "法律检索结果",
                "原计划需要增加赔偿责任分析");

        assertEquals(
                "分析劳动合同",
                context.goal());

        assertEquals(
                "原计划需要增加赔偿责任分析",
                context.reflectionSummary());
    }

    @Test
    void shouldNormalizeOptionalValues() {

        ReplanningPromptContext context = new ReplanningPromptContext(
                "分析劳动合同",
                null,
                null,
                null,
                "需要重新规划");

        assertEquals(
                "无",
                context.reasonSummary());

        assertEquals(
                "无",
                context.currentPlan());

        assertEquals(
                "无",
                context.observations());
    }

    @Test
    void shouldExposeTemplateVariables() {

        ReplanningPromptContext context = new ReplanningPromptContext(
                "分析劳动合同",
                "目标理解",
                "当前计划",
                "执行结果",
                "需要调整计划");

        Map<String, Object> variables = context.toVariables();

        assertEquals(
                "分析劳动合同",
                variables.get("goal"));

        assertEquals(
                "当前计划",
                variables.get("currentPlan"));

        assertEquals(
                "执行结果",
                variables.get("observations"));

        assertEquals(
                "需要调整计划",
                variables.get("reflectionSummary"));
    }

    @Test
    void shouldRejectBlankGoal() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new ReplanningPromptContext(
                        " ",
                        null,
                        null,
                        null,
                        "需要调整计划"));
    }

    @Test
    void shouldRejectBlankReflectionSummary() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new ReplanningPromptContext(
                        "分析劳动合同",
                        null,
                        null,
                        null,
                        "   "));
    }
}