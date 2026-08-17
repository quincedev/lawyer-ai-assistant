package com.quince.lawyeraiassistant.agent.prompt.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentTask;
import com.quince.lawyeraiassistant.agent.model.RuntimeReasonObservation;
import com.quince.lawyeraiassistant.agent.prompt.builder.ReflectionPromptContextBuilder;
import com.quince.lawyeraiassistant.security.legal.evidence.LegalEvidencePromptFormatter;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReflectionPromptContextTest {

        private ReflectionPromptContextBuilder builder;

        @BeforeEach
        void setUp() {

                builder = new ReflectionPromptContextBuilder(
                                new LegalEvidencePromptFormatter());
        }

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

        @Test
        void shouldIncludeRuntimeReasonObservationInReflectionContext() {

                RuntimeReasonObservation reasonObservation = RuntimeReasonObservation.of(
                                "task-1",
                                "AI Agent 可以围绕目标自主选择下一步行动");

                AgentContext context = AgentContext.from(
                                "分析 Agent 和 Workflow 的区别")
                                .appendRuntimeReasonObservation(
                                                reasonObservation);

                AgentTask task = AgentTask.pending(
                                "task-1",
                                "梳理 AI Agent 的核心特征");

                ReflectionPromptContext result = builder.build(
                                context,
                                task);

                assertTrue(
                                result.observations()
                                                .contains(
                                                                "Type: REASON"));

                assertTrue(
                                result.observations()
                                                .contains(
                                                                "Task: task-1"));

                assertTrue(
                                result.observations()
                                                .contains(
                                                                "AI Agent 可以围绕目标自主选择下一步行动"));
        }
}
