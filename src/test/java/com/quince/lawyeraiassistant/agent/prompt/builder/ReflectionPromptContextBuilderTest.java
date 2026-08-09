package com.quince.lawyeraiassistant.agent.prompt.builder;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentTask;
import com.quince.lawyeraiassistant.agent.model.RuntimeReasonObservation;
import com.quince.lawyeraiassistant.agent.model.ToolObservation;
import com.quince.lawyeraiassistant.agent.prompt.model.ReflectionPromptContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReflectionPromptContextBuilderTest {

        private final ReflectionPromptContextBuilder builder = new ReflectionPromptContextBuilder();

        @Test
        void shouldBuildReflectionPromptContext() {

                AgentContext context = AgentContext.from(
                                "分析违法解除劳动合同")
                                .appendObservation(
                                                ToolObservation.success(
                                                                "task-1",
                                                                "searchLegalKnowledge",
                                                                "违法解除可能涉及赔偿金责任"));

                AgentTask task = AgentTask.pending(
                                "task-1",
                                "查询违法解除的法律责任");

                ReflectionPromptContext result = builder.build(
                                context,
                                task);

                assertEquals(
                                "分析违法解除劳动合同",
                                result.goal());

                assertEquals(
                                "task-1",
                                result.taskId());

                assertEquals(
                                "查询违法解除的法律责任",
                                result.taskDescription());

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
        void shouldSupportContextWithoutObservations() {

                AgentContext context = AgentContext.from(
                                "分析劳动合同");

                AgentTask task = AgentTask.pending(
                                "task-1",
                                "分析合同风险");

                ReflectionPromptContext result = builder.build(
                                context,
                                task);

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
                                                                "task-1",
                                                                "现有材料不足以确认解除是否合法"));

                AgentTask task = AgentTask.pending(
                                "task-1",
                                "判断劳动合同解除是否合法");

                ReflectionPromptContext result = builder.build(
                                context,
                                task);

                assertTrue(result.observations().contains("Type: REASON"));
                assertTrue(result.observations().contains("Task: task-1"));
                assertTrue(result.observations().contains("现有材料不足以确认解除是否合法"));
        }

        @Test
        void shouldRejectNullContext() {

                AgentTask task = AgentTask.pending(
                                "task-1",
                                "分析合同风险");

                assertThrows(
                                NullPointerException.class,
                                () -> builder.build(
                                                null,
                                                task));
        }

        @Test
        void shouldRejectNullTask() {

                AgentContext context = AgentContext.from(
                                "分析劳动合同");

                assertThrows(
                                NullPointerException.class,
                                () -> builder.build(
                                                context,
                                                null));
        }
}
