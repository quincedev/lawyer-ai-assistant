package com.quince.lawyeraiassistant.agent.prompt.builder;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentPlan;
import com.quince.lawyeraiassistant.agent.model.AgentTask;
import com.quince.lawyeraiassistant.agent.model.ToolObservation;
import com.quince.lawyeraiassistant.agent.prompt.model.RuntimeReasonPromptContext;
import com.quince.lawyeraiassistant.security.legal.evidence.LegalEvidencePromptFormatter;
import com.quince.lawyeraiassistant.security.SecurityTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.quince.lawyeraiassistant.security.legal.TestLegalSecurityContexts.toolResult;

class RuntimeReasonPromptContextBuilderTest {

        private RuntimeReasonPromptContextBuilder builder;

        @BeforeEach
        void setUp() {

                builder = new RuntimeReasonPromptContextBuilder(
                                new LegalEvidencePromptFormatter());
        }

        @SecurityTest
        @Test
        void shouldBuildRuntimeReasonPromptContext() {

                AgentTask task = AgentTask.pending(
                                "task-2",
                                "分析竞业限制条款");

                AgentPlan plan = AgentPlan.from(
                                List.of(
                                                AgentTask.pending(
                                                                "task-1",
                                                                "读取劳动合同"),
                                                task));

                AgentContext context = AgentContext.from(
                                "分析劳动合同")
                                .toBuilder()
                                .agentPlan(
                                                plan)
                                .build()
                                .appendObservation(
                                                ToolObservation.success(
                                                                "task-1",
                                                                "contractReader",
                                                                "合同约定竞业限制24个月",
                                                                toolResult()));

                RuntimeReasonPromptContext result = builder.build(
                                context,
                                task);

                assertEquals(
                                "分析劳动合同",
                                result.goal());

                assertTrue(
                                result.currentTask()
                                                .contains(
                                                                "task-2"));

                assertTrue(
                                result.currentTask()
                                                .contains(
                                                                "分析竞业限制条款"));

                assertTrue(
                                result.currentPlan()
                                                .contains(
                                                                "task-1"));

                assertTrue(
                                result.observations()
                                                .contains(
                                                                "竞业限制24个月"));

                assertTrue(
                                result.observations()
                                                .contains(
                                                                "Source: TOOL_RESULT"));

                assertTrue(
                                result.observations()
                                                .contains(
                                                                "Trust-Level: UNTRUSTED"));

                assertTrue(
                                result.observations()
                                                .contains(
                                                                "Interpretation: DATA_ONLY"));
        }

        @Test
        void shouldSupportNoObservations() {

                AgentTask task = AgentTask.pending(
                                "task-1",
                                "分析合同");

                AgentPlan plan = AgentPlan.from(
                                List.of(
                                                task));

                AgentContext context = AgentContext.from(
                                "分析劳动合同")
                                .toBuilder()
                                .agentPlan(
                                                plan)
                                .build();

                RuntimeReasonPromptContext result = builder.build(
                                context,
                                task);

                assertEquals(
                                "无",
                                result.observations());
        }

        @Test
        void shouldRejectTaskNotInCurrentPlan() {

                AgentTask planTask = AgentTask.pending(
                                "task-1",
                                "读取合同");

                AgentTask anotherTask = AgentTask.pending(
                                "task-2",
                                "分析合同");

                AgentContext context = AgentContext.from(
                                "分析劳动合同")
                                .toBuilder()
                                .agentPlan(
                                                AgentPlan.from(
                                                                List.of(
                                                                                planTask)))
                                .build();

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> builder.build(
                                                context,
                                                anotherTask));

                assertEquals(
                                "AgentTask must belong to current AgentPlan",
                                exception.getMessage());
        }
}
