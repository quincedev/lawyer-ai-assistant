package com.quince.lawyeraiassistant.agent.prompt.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReflectionPromptContextTest {

    @Test
    void shouldCreatePromptContext() {

        ReflectionPromptContext context = new ReflectionPromptContext(
                "分析劳动合同",
                "分析合同风险",
                "task-1",
                "查询相关法律依据",
                "task-1 | RUNNING",
                "法律检索结果");

        assertEquals(
                "分析劳动合同",
                context.goal());

        assertEquals(
                "task-1",
                context.taskId());

        assertEquals(
                "查询相关法律依据",
                context.taskDescription());
    }

    @Test
    void shouldNormalizeOptionalValues() {

        ReflectionPromptContext context = new ReflectionPromptContext(
                "分析劳动合同",
                null,
                "task-1",
                "分析风险",
                null,
                null);

        assertEquals(
                "无",
                context.reasonSummary());

        assertEquals(
                "无",
                context.plan());

        assertEquals(
                "无",
                context.observations());
    }

    @Test
    void shouldExposeTemplateVariables() {

        ReflectionPromptContext context = new ReflectionPromptContext(
                "分析劳动合同",
                "目标理解",
                "task-1",
                "查询法律依据",
                "执行计划",
                "执行结果");

        Map<String, Object> variables = context.toVariables();

        assertEquals(
                "分析劳动合同",
                variables.get("goal"));

        assertEquals(
                "task-1",
                variables.get("taskId"));

        assertEquals(
                "执行结果",
                variables.get("observations"));
    }

    @Test
    void shouldRejectBlankTaskId() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new ReflectionPromptContext(
                        "分析劳动合同",
                        null,
                        "   ",
                        "分析风险",
                        null,
                        null));
    }

    @Test
    void shouldRejectBlankTaskDescription() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new ReflectionPromptContext(
                        "分析劳动合同",
                        null,
                        "task-1",
                        "   ",
                        null,
                        null));
    }
}