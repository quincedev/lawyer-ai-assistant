package com.quince.lawyeraiassistant.agent.prompt.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FinalAnswerPromptContextTest {

        @Test
        void shouldNormalizeOptionalValues() {

                FinalAnswerPromptContext context = new FinalAnswerPromptContext(
                                "分析劳动合同",
                                null,
                                null,
                                null);

                assertEquals(
                                "分析劳动合同",
                                context.goal());

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

                FinalAnswerPromptContext context = new FinalAnswerPromptContext(
                                "分析劳动合同",
                                "分析合同风险",
                                "task-1",
                                "法律检索结果");

                Map<String, Object> variables = context.toVariables();

                assertEquals(
                                "分析劳动合同",
                                variables.get("goal"));

                assertEquals(
                                "分析合同风险",
                                variables.get("reasonSummary"));

                assertEquals(
                                "task-1",
                                variables.get("plan"));

                assertEquals(
                                "法律检索结果",
                                variables.get("observations"));
        }

        @Test
        void shouldRejectBlankGoal() {

                assertThrows(
                                IllegalArgumentException.class,
                                () -> new FinalAnswerPromptContext(
                                                "   ",
                                                null,
                                                null,
                                                null));
        }
}