package com.quince.lawyeraiassistant.agent.prompt.builder;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.RuntimeReasonObservation;
import com.quince.lawyeraiassistant.agent.model.ToolObservation;
import com.quince.lawyeraiassistant.agent.prompt.model.FinalAnswerPromptContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinalAnswerPromptContextBuilderTest {

    private final FinalAnswerPromptContextBuilder builder = new FinalAnswerPromptContextBuilder();

    @Test
    void shouldBuildMinimalPromptContext() {

        AgentContext context = AgentContext.from(
                "分析劳动合同");

        FinalAnswerPromptContext result = builder.build(
                context);

        assertEquals(
                "分析劳动合同",
                result.goal());

        assertEquals(
                "无",
                result.reasonSummary());

        assertEquals(
                "无",
                result.plan());

        assertEquals(
                "无",
                result.observations());
    }

    @Test
    void shouldIncludeSuccessfulObservation() {

        AgentContext context = AgentContext.from(
                "分析违法解除劳动合同")
                .appendObservation(
                        ToolObservation.success(
                                "task-1",
                                "searchLegalKnowledge",
                                "违法解除劳动合同可能涉及赔偿金责任"));

        FinalAnswerPromptContext result = builder.build(
                context);

        assertTrue(
                result.observations()
                        .contains(
                                "task-1"));

        assertTrue(
                result.observations()
                        .contains(
                                "searchLegalKnowledge"));

        assertTrue(
                result.observations()
                        .contains(
                                "赔偿金责任"));
    }

    @Test
    void shouldIncludeFailedObservation() {

        AgentContext context = AgentContext.from(
                "分析劳动合同")
                .appendObservation(
                        ToolObservation.failure(
                                "task-1",
                                "searchLegalKnowledge",
                                "Knowledge retrieval failed"));

        FinalAnswerPromptContext result = builder.build(
                context);

        assertTrue(
                result.observations()
                        .contains(
                                "FAILED"));

        assertTrue(
                result.observations()
                        .contains(
                                "Knowledge retrieval failed"));
    }

    @Test
    void shouldIncludeRuntimeReasonObservations() {

        AgentContext context = AgentContext.from(
                "分析劳动合同")
                .appendRuntimeReasonObservation(
                        RuntimeReasonObservation.of(
                                "task-2",
                                "竞业限制期限可能超过合理范围"));

        FinalAnswerPromptContext result = builder.build(
                context);

        assertTrue(result.observations().contains("Type: REASON"));
        assertTrue(result.observations().contains("Task: task-2"));
        assertTrue(result.observations().contains("竞业限制期限可能超过合理范围"));
    }

    @Test
    void shouldRejectNullContext() {

        assertThrows(
                NullPointerException.class,
                () -> builder.build(
                        null));
    }
}
