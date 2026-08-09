package com.quince.lawyeraiassistant.agent.dto;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentPlan;
import com.quince.lawyeraiassistant.agent.model.AgentStatus;
import com.quince.lawyeraiassistant.agent.model.AgentTask;
import com.quince.lawyeraiassistant.agent.model.AgentTaskStatus;
import com.quince.lawyeraiassistant.agent.model.ReasonResult;
import com.quince.lawyeraiassistant.agent.model.ToolObservation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentResponseTest {

        @Test
        void shouldCreateResponseFromCompleteAgentContext() {
                AgentContext context = AgentContext.builder()
                                .goal(
                                                "分析劳动合同并生成律师意见书")
                                .reasonResult(
                                                ReasonResult.from(
                                                                "用户希望分析劳动合同并生成律师意见书。"))
                                .agentPlan(
                                                AgentPlan.from(
                                                                List.of(
                                                                                AgentTask.pending(
                                                                                                "task-1",
                                                                                                "读取劳动合同"),
                                                                                AgentTask.pending(
                                                                                                "task-2",
                                                                                                "识别法律风险"))))
                                .observations(
                                                List.of(
                                                                ToolObservation.success(
                                                                                "task-1",
                                                                                "searchLegalKnowledge",
                                                                                "检索到劳动合同法相关规定。")))
                                .status(
                                                AgentStatus.RUNNING)
                                .executionLogs(
                                                List.of(
                                                                "Reason completed",
                                                                "Planning completed"))
                                .build();

                AgentResponse response = AgentResponse.from(
                                context);

                assertEquals(
                                "分析劳动合同并生成律师意见书",
                                response.goal());

                assertEquals(
                                "用户希望分析劳动合同并生成律师意见书。",
                                response.reasonSummary());

                assertEquals(
                                AgentStatus.RUNNING,
                                response.status());

                assertEquals(
                                2,
                                response.plan()
                                                .size());

                AgentTaskResponse firstTask = response.plan()
                                .get(0);

                assertEquals(
                                "task-1",
                                firstTask.id());

                assertEquals(
                                "读取劳动合同",
                                firstTask.description());

                assertEquals(
                                AgentTaskStatus.PENDING,
                                firstTask.status());

                assertEquals(
                                List.of(
                                                "Reason completed",
                                                "Planning completed"),
                                response.executionLogs());
                assertEquals(
                                1,
                                response.observations()
                                                .size());

                ToolObservationResponse observation = response.observations()
                                .getFirst();

                assertEquals(
                                "task-1",
                                observation.taskId());

                assertEquals(
                                "searchLegalKnowledge",
                                observation.toolName());

                assertTrue(
                                observation.success());

                assertEquals(
                                "检索到劳动合同法相关规定。",
                                observation.content());

                assertNull(
                                observation.errorMessage());
        }

        @Test
        void shouldReturnEmptyPlanWhenPlanningHasNotExecuted() {
                AgentContext context = AgentContext.from(
                                "分析劳动合同");

                AgentResponse response = AgentResponse.from(
                                context);

                assertNull(
                                response.reasonSummary());

                assertTrue(
                                response.plan()
                                                .isEmpty());

                assertEquals(
                                AgentStatus.CREATED,
                                response.status());
                assertTrue(
                                response.observations()
                                                .isEmpty());
        }

        @Test
        void shouldCreateDefensiveCopiesOfCollections() {
                List<AgentTaskResponse> plan = new java.util.ArrayList<>();

                plan.add(
                                new AgentTaskResponse(
                                                "task-1",
                                                "读取劳动合同",
                                                AgentTaskStatus.PENDING));

                List<String> logs = new java.util.ArrayList<>();

                logs.add(
                                "Reason completed");

                List<ToolObservationResponse> observations = new java.util.ArrayList<>();

                observations.add(
                                new ToolObservationResponse(
                                                "task-1",
                                                "searchLegalKnowledge",
                                                true,
                                                "检索结果",
                                                null));

                AgentResponse response = new AgentResponse(
                                "分析劳动合同",
                                "用户希望分析劳动合同。",
                                plan,
                                observations,
                                null,
                                AgentStatus.RUNNING,
                                logs);

                plan.clear();
                logs.clear();

                assertEquals(
                                1,
                                response.observations()
                                                .size());

                assertThrows(
                                UnsupportedOperationException.class,
                                () -> response.observations()
                                                .add(
                                                                new ToolObservationResponse(
                                                                                "task-2",
                                                                                "searchLegalKnowledge",
                                                                                true,
                                                                                "其他结果",
                                                                                null)));

                assertEquals(
                                1,
                                response.plan()
                                                .size());

                assertEquals(
                                List.of(
                                                "Reason completed"),
                                response.executionLogs());

                assertThrows(
                                UnsupportedOperationException.class,
                                () -> response.plan()
                                                .add(
                                                                new AgentTaskResponse(
                                                                                "task-2",
                                                                                "分析风险",
                                                                                AgentTaskStatus.PENDING)));

                assertThrows(
                                UnsupportedOperationException.class,
                                () -> response.executionLogs()
                                                .add(
                                                                "Illegal log"));
        }

        @Test
        void shouldRejectNullContext() {
                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> AgentResponse.from(
                                                null));

                assertEquals(
                                "AgentContext must not be null",
                                exception.getMessage());
        }

        @Test
        void shouldMapFinalAnswerFromContext() {

                AgentContext context = AgentContext.from(
                                "分析劳动合同")
                                .withFinalAnswer(
                                                "根据现有材料，合同存在以下风险。");

                AgentResponse response = AgentResponse.from(
                                context);

                assertEquals(
                                "根据现有材料，合同存在以下风险。",
                                response.finalAnswer());
        }

        @Test
        void shouldAllowNullFinalAnswerBeforeGeneration() {

                AgentContext context = AgentContext.from(
                                "分析劳动合同");

                AgentResponse response = AgentResponse.from(
                                context);

                assertNull(
                                response.finalAnswer());
        }
}