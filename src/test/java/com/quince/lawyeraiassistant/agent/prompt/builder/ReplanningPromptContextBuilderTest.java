package com.quince.lawyeraiassistant.agent.prompt.builder;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.ReflectionDecision;
import com.quince.lawyeraiassistant.agent.model.ReflectionResult;
import com.quince.lawyeraiassistant.agent.model.RuntimeReasonObservation;
import com.quince.lawyeraiassistant.agent.model.ToolObservation;
import com.quince.lawyeraiassistant.agent.prompt.model.ReplanningPromptContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplanningPromptContextBuilderTest {

    private final ReplanningPromptContextBuilder builder = new ReplanningPromptContextBuilder();

    @Test
    void shouldBuildReplanningPromptContext() {

        AgentContext context = AgentContext.from(
                "分析违法解除劳动合同")
                .appendObservation(
                        ToolObservation.success(
                                "task-1",
                                "searchLegalKnowledge",
                                "发现需要进一步分析赔偿责任"));

        ReflectionResult reflectionResult = ReflectionResult.of(
                ReflectionDecision.REPLAN,
                "原计划没有覆盖违法解除赔偿责任");

        ReplanningPromptContext result = builder.build(
                context,
                reflectionResult);

        assertEquals(
                "分析违法解除劳动合同",
                result.goal());

        assertEquals(
                "原计划没有覆盖违法解除赔偿责任",
                result.reflectionSummary());

        assertTrue(
                result.observations()
                        .contains(
                                "searchLegalKnowledge"));

        assertTrue(
                result.observations()
                        .contains(
                                "赔偿责任"));
    }

    @Test
    void shouldSupportContextWithoutObservations() {

        AgentContext context = AgentContext.from(
                "分析劳动合同");

        ReflectionResult reflectionResult = ReflectionResult.of(
                ReflectionDecision.REPLAN,
                "需要调整计划");

        ReplanningPromptContext result = builder.build(
                context,
                reflectionResult);

        assertEquals(
                "无",
                result.observations());
    }

    @Test
    void shouldIncludeRuntimeReasonObservations() {

        AgentContext context = AgentContext.from(
                "分析劳动合同")
                .appendRuntimeReasonObservation(
                        RuntimeReasonObservation.of(
                                "task-2",
                                "原计划缺少对赔偿责任的分析"));

        ReflectionResult reflectionResult = ReflectionResult.of(
                ReflectionDecision.REPLAN,
                "需要补充赔偿责任任务");

        ReplanningPromptContext result = builder.build(
                context,
                reflectionResult);

        assertTrue(result.observations().contains("Type: REASON"));
        assertTrue(result.observations().contains("Task: task-2"));
        assertTrue(result.observations().contains("原计划缺少对赔偿责任的分析"));
    }

    @Test
    void shouldRejectNullContext() {

        ReflectionResult reflectionResult = ReflectionResult.of(
                ReflectionDecision.REPLAN,
                "需要调整计划");

        assertThrows(
                NullPointerException.class,
                () -> builder.build(
                        null,
                        reflectionResult));
    }

    @Test
    void shouldRejectNullReflectionResult() {

        AgentContext context = AgentContext.from(
                "分析劳动合同");

        assertThrows(
                NullPointerException.class,
                () -> builder.build(
                        context,
                        null));
    }
}
