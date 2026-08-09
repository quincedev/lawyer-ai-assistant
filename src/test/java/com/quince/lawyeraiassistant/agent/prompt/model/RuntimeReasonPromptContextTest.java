package com.quince.lawyeraiassistant.agent.prompt.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RuntimeReasonPromptContextTest {

    @Test
    void shouldCreateRuntimeReasonPromptContext() {

        RuntimeReasonPromptContext context = new RuntimeReasonPromptContext(
                "分析劳动合同",
                "task-2 | RUNNING | 分析竞业限制条款",
                "task-1 | COMPLETED | 读取合同",
                "已发现竞业限制24个月");

        assertEquals(
                "分析劳动合同",
                context.goal());

        assertEquals(
                "task-2 | RUNNING | 分析竞业限制条款",
                context.currentTask());

        assertEquals(
                "已发现竞业限制24个月",
                context.observations());
    }

    @Test
    void shouldNormalizeOptionalValues() {

        RuntimeReasonPromptContext context = new RuntimeReasonPromptContext(
                "分析劳动合同",
                "task-1 | RUNNING | 分析条款",
                null,
                null);

        assertEquals(
                "无",
                context.currentPlan());

        assertEquals(
                "无",
                context.observations());
    }

    @Test
    void shouldExposeTemplateVariables() {

        RuntimeReasonPromptContext context = new RuntimeReasonPromptContext(
                "分析劳动合同",
                "当前任务",
                "当前计划",
                "已有结果");

        Map<String, Object> variables = context.toVariables();

        assertEquals(
                "分析劳动合同",
                variables.get("goal"));

        assertEquals(
                "当前任务",
                variables.get("currentTask"));

        assertEquals(
                "当前计划",
                variables.get("currentPlan"));

        assertEquals(
                "已有结果",
                variables.get("observations"));
    }

    @Test
    void shouldRejectBlankGoal() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new RuntimeReasonPromptContext(
                        " ",
                        "当前任务",
                        null,
                        null));
    }

    @Test
    void shouldRejectBlankCurrentTask() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new RuntimeReasonPromptContext(
                        "分析劳动合同",
                        " ",
                        null,
                        null));
    }
}