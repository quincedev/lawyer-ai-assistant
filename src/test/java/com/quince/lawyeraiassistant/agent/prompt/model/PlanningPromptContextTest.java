package com.quince.lawyeraiassistant.agent.prompt.model;

import com.quince.lawyeraiassistant.agent.model.ReasonResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlanningPromptContextTest {

        @Test
        void shouldCreatePlanningPromptContext() {

                ReasonResult reasonResult = ReasonResult.from(
                                "需要分析劳动合同解除涉及的法律问题");

                PlanningPromptContext context = PlanningPromptContext.from(
                                "分析违法解除劳动合同",
                                reasonResult,
                                "优先检索法律知识库",
                                "searchLegalKnowledge");

                assertEquals(
                                "分析违法解除劳动合同",
                                context.getGoal());

                assertSame(
                                reasonResult,
                                context.getReasonResult());

                assertEquals(
                                "优先检索法律知识库",
                                context.getSkillInstructions());

                assertEquals(
                                "searchLegalKnowledge",
                                context.getAvailableTools());
        }

        @Test
        void shouldNormalizeGoal() {

                ReasonResult reasonResult = ReasonResult.from(
                                "分析劳动合同风险");

                PlanningPromptContext context = PlanningPromptContext.from(
                                "   分析劳动合同   ",
                                reasonResult,
                                "执行法律研究",
                                "searchLegalKnowledge");

                assertEquals(
                                "分析劳动合同",
                                context.getGoal());
        }

        @Test
        void shouldNormalizeSkillInstructions() {

                ReasonResult reasonResult = ReasonResult.from(
                                "分析劳动合同风险");

                PlanningPromptContext context = PlanningPromptContext.from(
                                "分析劳动合同",
                                reasonResult,
                                "   优先检索法律知识库   ",
                                "searchLegalKnowledge");

                assertEquals(
                                "优先检索法律知识库",
                                context.getSkillInstructions());
        }

        @Test
        void shouldUseNoneWhenSkillInstructionsIsNull() {

                ReasonResult reasonResult = ReasonResult.from(
                                "分析普通问题");

                PlanningPromptContext context = PlanningPromptContext.from(
                                "处理普通问题",
                                reasonResult,
                                null,
                                null);

                assertEquals(
                                "无",
                                context.getSkillInstructions());
        }

        @Test
        void shouldUseNoneWhenSkillInstructionsIsBlank() {

                ReasonResult reasonResult = ReasonResult.from(
                                "分析普通问题");

                PlanningPromptContext context = PlanningPromptContext.from(
                                "处理普通问题",
                                reasonResult,
                                "   ",
                                null);

                assertEquals(
                                "无",
                                context.getSkillInstructions());
        }

        @Test
        void shouldExposeTemplateVariables() {

                ReasonResult reasonResult = ReasonResult.from(
                                "需要判断劳动合同解除是否合法");

                PlanningPromptContext context = PlanningPromptContext.from(
                                "分析违法解除劳动合同",
                                reasonResult,
                                "优先检索法律知识库",
                                "searchLegalKnowledge");

                Map<String, Object> variables = context.toVariables();

                assertEquals(
                                "分析违法解除劳动合同",
                                variables.get("goal"));

                assertEquals(
                                "需要判断劳动合同解除是否合法",
                                variables.get("reasonSummary"));

                assertEquals(
                                "优先检索法律知识库",
                                variables.get(
                                                "skillInstructions"));

                assertEquals(
                                "searchLegalKnowledge",
                                variables.get(
                                                "availableTools"));
        }

        @Test
        void shouldExposeNoneWhenNoSkillInstructions() {

                ReasonResult reasonResult = ReasonResult.from(
                                "分析普通问题");

                PlanningPromptContext context = PlanningPromptContext.from(
                                "处理普通问题",
                                reasonResult,
                                null,
                                null);

                Map<String, Object> variables = context.toVariables();

                assertEquals(
                                "无",
                                variables.get(
                                                "skillInstructions"));
        }

        @Test
        void shouldRejectNullGoal() {

                ReasonResult reasonResult = ReasonResult.from(
                                "分析法律问题");

                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> PlanningPromptContext.from(
                                                null,
                                                reasonResult,
                                                "执行法律研究",
                                                "searchLegalKnowledge"));

                assertEquals(
                                "Goal must not be null",
                                exception.getMessage());
        }

        @Test
        void shouldRejectBlankGoal() {

                ReasonResult reasonResult = ReasonResult.from(
                                "分析法律问题");

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> PlanningPromptContext.from(
                                                "   ",
                                                reasonResult,
                                                "执行法律研究",
                                                "searchLegalKnowledge"));

                assertEquals(
                                "Goal must not be blank",
                                exception.getMessage());
        }

        @Test
        void shouldRejectNullReasonResult() {

                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> PlanningPromptContext.from(
                                                "分析劳动合同",
                                                null,
                                                "执行法律研究",
                                                "searchLegalKnowledge"));

                assertEquals(
                                "ReasonResult must not be null",
                                exception.getMessage());
        }

        @Test
        void shouldNormalizeAvailableTools() {

                ReasonResult reasonResult = ReasonResult.from(
                                "分析法律问题");

                PlanningPromptContext context = PlanningPromptContext.from(
                                "分析劳动合同",
                                reasonResult,
                                "执行法律研究",
                                "   searchLegalKnowledge   ");

                assertEquals(
                                "searchLegalKnowledge",
                                context.getAvailableTools());
        }

        @Test
        void shouldUseNoneWhenAvailableToolsMissing() {

                ReasonResult reasonResult = ReasonResult.from(
                                "分析普通问题");

                PlanningPromptContext context = PlanningPromptContext.from(
                                "分析普通问题",
                                reasonResult,
                                null,
                                null);

                assertEquals(
                                "无",
                                context.getAvailableTools());

                assertEquals(
                                "无",
                                context.toVariables()
                                                .get(
                                                                "availableTools"));
        }
}