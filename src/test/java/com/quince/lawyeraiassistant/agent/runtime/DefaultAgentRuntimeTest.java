package com.quince.lawyeraiassistant.agent.runtime;

import com.quince.lawyeraiassistant.agent.action.AgentActionSelector;
import com.quince.lawyeraiassistant.agent.model.AgentAction;
import com.quince.lawyeraiassistant.agent.model.AgentActionExecutionResult;
import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentPlan;
import com.quince.lawyeraiassistant.agent.model.AgentStatus;
import com.quince.lawyeraiassistant.agent.model.AgentTask;
import com.quince.lawyeraiassistant.agent.model.AgentTaskStatus;
import com.quince.lawyeraiassistant.agent.model.ReflectionDecision;
import com.quince.lawyeraiassistant.agent.model.ReflectionResult;
import com.quince.lawyeraiassistant.agent.model.ToolAction;
import com.quince.lawyeraiassistant.agent.model.ToolObservation;
import com.quince.lawyeraiassistant.agent.operator.AgentActionExecutionOperator;
import com.quince.lawyeraiassistant.agent.pipeline.AgentPipeline;
import com.quince.lawyeraiassistant.agent.service.AgentFinalAnswerService;
import com.quince.lawyeraiassistant.agent.service.AgentReflectionService;
import com.quince.lawyeraiassistant.agent.service.AgentReplanningService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DefaultAgentRuntimeTest {

        private AgentPipeline agentPipeline;

        private AgentActionSelector actionSelector;

        private AgentActionExecutionOperator actionExecutionOperator;

        private AgentReflectionService reflectionService;

        private AgentReplanningService replanningService;

        private AgentFinalAnswerService finalAnswerService;

        @BeforeEach
        void setUp() {

                agentPipeline = mock(
                                AgentPipeline.class);

                actionSelector = mock(
                                AgentActionSelector.class);

                actionExecutionOperator = mock(
                                AgentActionExecutionOperator.class);

                reflectionService = mock(
                                AgentReflectionService.class);

                replanningService = mock(
                                AgentReplanningService.class);

                finalAnswerService = mock(
                                AgentFinalAnswerService.class);
        }

        /*
         * =========================================================
         * Basic Runtime Regression
         * =========================================================
         */

        @Test
        void shouldApplyToolActionResult() {

                AgentContext initialized = initializedContext(
                                AgentTask.pending(
                                                "task-1",
                                                "查询法律依据"));

                ToolAction toolAction = ToolAction.of(
                                "task-1",
                                "searchLegalKnowledge",
                                Map.of(
                                                "question",
                                                "违法解除责任"));

                ToolObservation observation = ToolObservation.success(
                                "task-1",
                                "searchLegalKnowledge",
                                "违法解除可能承担赔偿金责任");

                when(
                                agentPipeline.execute(
                                                any()))
                                .thenReturn(
                                                initialized);

                when(
                                actionSelector.select(
                                                any(),
                                                any()))
                                .thenReturn(
                                                AgentAction.tool(
                                                                toolAction));

                when(
                                actionExecutionOperator.execute(
                                                any(),
                                                any(),
                                                any()))
                                .thenReturn(
                                                AgentActionExecutionResult.tool(
                                                                observation));

                when(
                                reflectionService.reflect(
                                                any(),
                                                any()))
                                .thenReturn(
                                                ReflectionResult.of(
                                                                ReflectionDecision.CONTINUE,
                                                                "当前任务已经完成"));

                when(
                                finalAnswerService.generate(
                                                any()))
                                .thenReturn(
                                                "最终法律意见");

                AgentContext result = runtime(10)
                                .run(
                                                AgentContext.from(
                                                                "分析劳动合同"));

                assertEquals(
                                AgentTaskStatus.COMPLETED,
                                task(
                                                result,
                                                "task-1")
                                                .getStatus());

                assertEquals(
                                List.of(
                                                observation),
                                result.getObservations());

                assertEquals(
                                "最终法律意见",
                                result.getFinalAnswer());

                assertEquals(
                                AgentStatus.FINISHED,
                                result.getStatus());

                assertTrue(
                                result.getExecutionLogs()
                                                .contains(
                                                                "Tool action completed: task-1"));

                verify(
                                reflectionService,
                                times(1))
                                .reflect(
                                                any(),
                                                any());
        }

        @Test
        void shouldApplyReasonActionResult() {

                AgentContext initialized = initializedContext(
                                AgentTask.pending(
                                                "task-1",
                                                "分析现有材料"));

                when(
                                agentPipeline.execute(
                                                any()))
                                .thenReturn(
                                                initialized);

                when(
                                actionSelector.select(
                                                any(),
                                                any()))
                                .thenReturn(
                                                AgentAction.reason(
                                                                "task-1"));

                when(
                                actionExecutionOperator.execute(
                                                any(),
                                                any(),
                                                any()))
                                .thenReturn(
                                                AgentActionExecutionResult.reason(
                                                                "现有材料不足以确认解除是否合法"));

                when(
                                reflectionService.reflect(
                                                any(),
                                                any()))
                                .thenReturn(
                                                ReflectionResult.of(
                                                                ReflectionDecision.CONTINUE,
                                                                "当前分析任务已经完成"));

                when(
                                finalAnswerService.generate(
                                                any()))
                                .thenReturn(
                                                "需要补充解除通知书");

                AgentContext result = runtime(10)
                                .run(
                                                AgentContext.from(
                                                                "分析劳动合同"));

                assertEquals(
                                AgentTaskStatus.COMPLETED,
                                task(
                                                result,
                                                "task-1")
                                                .getStatus());

                assertEquals(
                                1,
                                result.getRuntimeReasonObservations()
                                                .size());

                assertEquals(
                                "task-1",
                                result.getRuntimeReasonObservations()
                                                .getFirst()
                                                .getTaskId());

                assertEquals(
                                "现有材料不足以确认解除是否合法",
                                result.getRuntimeReasonObservations()
                                                .getFirst()
                                                .getContent());

                assertEquals(
                                AgentStatus.FINISHED,
                                result.getStatus());

                verify(
                                reflectionService,
                                times(1))
                                .reflect(
                                                any(),
                                                any());
        }

        @Test
        void shouldFinishImmediatelyForFinalAnswerAction() {

                AgentContext initialized = initializedContext(
                                AgentTask.pending(
                                                "task-1",
                                                "生成最终答复"));

                when(
                                agentPipeline.execute(
                                                any()))
                                .thenReturn(
                                                initialized);

                when(
                                actionSelector.select(
                                                any(),
                                                any()))
                                .thenReturn(
                                                AgentAction.finalAnswer(
                                                                "task-1"));

                when(
                                actionExecutionOperator.execute(
                                                any(),
                                                any(),
                                                any()))
                                .thenReturn(
                                                AgentActionExecutionResult.finalAnswer(
                                                                "这是最终法律意见"));

                AgentContext result = runtime(10)
                                .run(
                                                AgentContext.from(
                                                                "分析劳动合同"));

                assertEquals(
                                AgentTaskStatus.COMPLETED,
                                task(
                                                result,
                                                "task-1")
                                                .getStatus());

                assertEquals(
                                "这是最终法律意见",
                                result.getFinalAnswer());

                assertEquals(
                                AgentStatus.FINISHED,
                                result.getStatus());

                verify(
                                reflectionService,
                                never())
                                .reflect(
                                                any(),
                                                any());

                verifyNoInteractions(
                                replanningService);

                verify(
                                finalAnswerService,
                                never())
                                .generate(
                                                any());
        }

        @Test
        void shouldGenerateFinalAnswerAsFallbackWhenPlanIsComplete() {

                AgentTask completedTask = AgentTask.pending(
                                "task-1",
                                "查询法律依据")
                                .withStatus(
                                                AgentTaskStatus.COMPLETED);

                AgentContext initialized = initializedContext(
                                completedTask);

                when(
                                agentPipeline.execute(
                                                any()))
                                .thenReturn(
                                                initialized);

                when(
                                finalAnswerService.generate(
                                                initialized))
                                .thenReturn(
                                                "基于执行结果生成的最终答复");

                AgentContext result = runtime(10)
                                .run(
                                                AgentContext.from(
                                                                "分析劳动合同"));

                assertEquals(
                                "基于执行结果生成的最终答复",
                                result.getFinalAnswer());

                assertEquals(
                                AgentStatus.FINISHED,
                                result.getStatus());

                assertTrue(
                                result.getExecutionLogs()
                                                .contains(
                                                                "Final answer generated"));

                verify(
                                actionSelector,
                                never())
                                .select(
                                                any(),
                                                any());

                verifyNoInteractions(
                                reflectionService);

                verifyNoInteractions(
                                replanningService);
        }

        /*
         * =========================================================
         * Sprint 3 - Reflection / Replanning Regression
         * =========================================================
         */

        @Test
        void shouldContinueToNextTaskWhenReflectionReturnsContinue() {

                AgentContext initialized = initializedContext(
                                AgentTask.pending(
                                                "task-1",
                                                "分析第一项问题"),
                                AgentTask.pending(
                                                "task-2",
                                                "分析第二项问题"));

                when(
                                agentPipeline.execute(
                                                any()))
                                .thenReturn(
                                                initialized);

                when(
                                actionSelector.select(
                                                any(),
                                                any()))
                                .thenReturn(
                                                AgentAction.reason(
                                                                "task-1"),
                                                AgentAction.reason(
                                                                "task-2"));

                when(
                                actionExecutionOperator.execute(
                                                any(),
                                                any(),
                                                any()))
                                .thenReturn(
                                                AgentActionExecutionResult.reason(
                                                                "第一项分析完成"),
                                                AgentActionExecutionResult.reason(
                                                                "第二项分析完成"));

                when(
                                reflectionService.reflect(
                                                any(),
                                                any()))
                                .thenReturn(
                                                ReflectionResult.of(
                                                                ReflectionDecision.CONTINUE,
                                                                "继续执行"),
                                                ReflectionResult.of(
                                                                ReflectionDecision.CONTINUE,
                                                                "全部完成"));

                when(
                                finalAnswerService.generate(
                                                any()))
                                .thenReturn(
                                                "最终法律意见");

                AgentContext result = runtime(10)
                                .run(
                                                AgentContext.from(
                                                                "分析劳动合同"));

                assertEquals(
                                AgentTaskStatus.COMPLETED,
                                task(
                                                result,
                                                "task-1")
                                                .getStatus());

                assertEquals(
                                AgentTaskStatus.COMPLETED,
                                task(
                                                result,
                                                "task-2")
                                                .getStatus());

                assertEquals(
                                AgentStatus.FINISHED,
                                result.getStatus());

                assertEquals(
                                "最终法律意见",
                                result.getFinalAnswer());

                verify(
                                actionSelector,
                                times(2))
                                .select(
                                                any(),
                                                any());

                verify(
                                reflectionService,
                                times(2))
                                .reflect(
                                                any(),
                                                any());
        }

        @Test
        void shouldRetrySameTaskWhenReflectionReturnsRetry() {

                AgentContext initialized = initializedContext(
                                AgentTask.pending(
                                                "task-1",
                                                "分析现有材料"));

                when(
                                agentPipeline.execute(
                                                any()))
                                .thenReturn(
                                                initialized);

                when(
                                actionSelector.select(
                                                any(),
                                                any()))
                                .thenReturn(
                                                AgentAction.reason(
                                                                "task-1"));

                when(
                                actionExecutionOperator.execute(
                                                any(),
                                                any(),
                                                any()))
                                .thenReturn(
                                                AgentActionExecutionResult.reason(
                                                                "第一次分析结果不充分"),
                                                AgentActionExecutionResult.reason(
                                                                "第二次分析结果已经充分"));

                when(
                                reflectionService.reflect(
                                                any(),
                                                any()))
                                .thenReturn(
                                                ReflectionResult.of(
                                                                ReflectionDecision.RETRY,
                                                                "第一次结果不足，需要重新分析"),
                                                ReflectionResult.of(
                                                                ReflectionDecision.CONTINUE,
                                                                "第二次结果已经满足要求"));

                when(
                                finalAnswerService.generate(
                                                any()))
                                .thenReturn(
                                                "最终法律意见");

                AgentContext result = runtime(10)
                                .run(
                                                AgentContext.from(
                                                                "分析劳动合同"));

                verify(
                                actionSelector,
                                times(2))
                                .select(
                                                any(),
                                                argThat(
                                                                currentTask -> "task-1".equals(
                                                                                currentTask.getId())));

                verify(
                                actionExecutionOperator,
                                times(2))
                                .execute(
                                                any(),
                                                any(),
                                                any());

                verify(
                                reflectionService,
                                times(2))
                                .reflect(
                                                any(),
                                                any());

                assertEquals(
                                AgentTaskStatus.COMPLETED,
                                task(
                                                result,
                                                "task-1")
                                                .getStatus());

                assertEquals(
                                2,
                                result.getRuntimeReasonObservations()
                                                .size());

                assertEquals(
                                AgentStatus.FINISHED,
                                result.getStatus());
        }

        @Test
        void shouldReplanAndExecuteNewPlanWhenReflectionReturnsReplan() {

                AgentContext initialized = initializedContext(
                                AgentTask.pending(
                                                "task-1",
                                                "分析现有法律依据"));

                AgentPlan replannedPlan = AgentPlan.from(
                                List.of(
                                                AgentTask.pending(
                                                                "task-2",
                                                                "补充检索司法解释")));

                when(
                                agentPipeline.execute(
                                                any()))
                                .thenReturn(
                                                initialized);

                when(
                                actionSelector.select(
                                                any(),
                                                argThat(
                                                                currentTask -> currentTask != null
                                                                                && "task-1".equals(
                                                                                currentTask.getId()))))
                                .thenReturn(
                                                AgentAction.reason(
                                                                "task-1"));

                when(
                                actionSelector.select(
                                                any(),
                                                argThat(
                                                                currentTask -> currentTask != null
                                                                                && "task-2".equals(
                                                                                currentTask.getId()))))
                                .thenReturn(
                                                AgentAction.reason(
                                                                "task-2"));

                when(
                                actionExecutionOperator.execute(
                                                any(),
                                                argThat(
                                                                currentTask -> currentTask != null
                                                                                && "task-1".equals(
                                                                                currentTask.getId())),
                                                any()))
                                .thenReturn(
                                                AgentActionExecutionResult.reason(
                                                                "现有法律依据不足"));

                when(
                                actionExecutionOperator.execute(
                                                any(),
                                                argThat(
                                                                currentTask -> currentTask != null
                                                                                && "task-2".equals(
                                                                                currentTask.getId())),
                                                any()))
                                .thenReturn(
                                                AgentActionExecutionResult.reason(
                                                                "已经补充司法解释"));

                ReflectionResult replanResult = ReflectionResult.of(
                                ReflectionDecision.REPLAN,
                                "需要补充司法解释");

                when(
                                reflectionService.reflect(
                                                any(),
                                                argThat(
                                                                currentTask -> currentTask != null
                                                                                && "task-1".equals(
                                                                                currentTask.getId()))))
                                .thenReturn(
                                                replanResult);

                when(
                                replanningService.replan(
                                                any(),
                                                eq(replanResult)))
                                .thenReturn(
                                                replannedPlan);

                when(
                                reflectionService.reflect(
                                                any(),
                                                argThat(
                                                                currentTask -> currentTask != null
                                                                                && "task-2".equals(
                                                                                currentTask.getId()))))
                                .thenReturn(
                                                ReflectionResult.of(
                                                                ReflectionDecision.CONTINUE,
                                                                "补充任务已经完成"));

                when(
                                finalAnswerService.generate(
                                                any()))
                                .thenReturn(
                                                "最终法律意见");

                AgentContext result = runtime(10)
                                .run(
                                                AgentContext.from(
                                                                "分析劳动合同"));

                verify(
                                replanningService,
                                times(1))
                                .replan(
                                                any(),
                                                eq(replanResult));

                verify(
                                actionSelector,
                                times(1))
                                .select(
                                                any(),
                                                argThat(
                                                                currentTask -> currentTask != null
                                                                                && "task-2".equals(
                                                                                currentTask.getId())));

                assertEquals(
                                AgentTaskStatus.COMPLETED,
                                task(
                                                result,
                                                "task-2")
                                                .getStatus());

                assertEquals(
                                AgentStatus.FINISHED,
                                result.getStatus());

                assertEquals(
                                "最终法律意见",
                                result.getFinalAnswer());
        }

        @Test
        void shouldGenerateFinalAnswerWhenReflectionReturnsFinish() {

                AgentContext initialized = initializedContext(
                                AgentTask.pending(
                                                "task-1",
                                                "分析现有材料"));

                when(
                                agentPipeline.execute(
                                                any()))
                                .thenReturn(
                                                initialized);

                when(
                                actionSelector.select(
                                                any(),
                                                any()))
                                .thenReturn(
                                                AgentAction.reason(
                                                                "task-1"));

                when(
                                actionExecutionOperator.execute(
                                                any(),
                                                any(),
                                                any()))
                                .thenReturn(
                                                AgentActionExecutionResult.reason(
                                                                "现有信息已经足够形成法律意见"));

                when(
                                reflectionService.reflect(
                                                any(),
                                                any()))
                                .thenReturn(
                                                ReflectionResult.of(
                                                                ReflectionDecision.FINISH,
                                                                "当前信息已经足够完成用户目标"));

                when(
                                finalAnswerService.generate(
                                                any()))
                                .thenReturn(
                                                "这是最终法律意见");

                AgentContext result = runtime(10)
                                .run(
                                                AgentContext.from(
                                                                "分析劳动合同"));

                assertEquals(
                                AgentStatus.FINISHED,
                                result.getStatus());

                assertEquals(
                                "这是最终法律意见",
                                result.getFinalAnswer());

                verify(
                                finalAnswerService,
                                times(1))
                                .generate(
                                                any());

                verifyNoInteractions(
                                replanningService);
        }

        @Test
        void shouldRetryFailedToolActionWhenReflectionReturnsRetry() {

                AgentContext initialized = initializedContext(
                                AgentTask.pending(
                                                "task-1",
                                                "查询法律依据"));

                ToolAction toolAction = ToolAction.of(
                                "task-1",
                                "searchLegalKnowledge",
                                Map.of(
                                                "question",
                                                "违法解除责任"));

                ToolObservation failedObservation = ToolObservation.failure(
                                "task-1",
                                "searchLegalKnowledge",
                                "Knowledge retrieval failed");

                ToolObservation successObservation = ToolObservation.success(
                                "task-1",
                                "searchLegalKnowledge",
                                "已经检索到相关法律条款");

                when(
                                agentPipeline.execute(
                                                any()))
                                .thenReturn(
                                                initialized);

                when(
                                actionSelector.select(
                                                any(),
                                                any()))
                                .thenReturn(
                                                AgentAction.tool(
                                                                toolAction));

                when(
                                actionExecutionOperator.execute(
                                                any(),
                                                any(),
                                                any()))
                                .thenReturn(
                                                AgentActionExecutionResult.tool(
                                                                failedObservation),
                                                AgentActionExecutionResult.tool(
                                                                successObservation));

                when(
                                reflectionService.reflect(
                                                any(),
                                                any()))
                                .thenReturn(
                                                ReflectionResult.of(
                                                                ReflectionDecision.RETRY,
                                                                "第一次 Tool 执行失败，需要重试"),
                                                ReflectionResult.of(
                                                                ReflectionDecision.CONTINUE,
                                                                "第二次 Tool 执行成功"));

                when(
                                finalAnswerService.generate(
                                                any()))
                                .thenReturn(
                                                "最终法律意见");

                AgentContext result = runtime(10)
                                .run(
                                                AgentContext.from(
                                                                "分析劳动合同"));

                verify(
                                actionExecutionOperator,
                                times(2))
                                .execute(
                                                any(),
                                                any(),
                                                any());

                verify(
                                reflectionService,
                                times(2))
                                .reflect(
                                                any(),
                                                any());

                assertEquals(
                                2,
                                result.getObservations()
                                                .size());

                assertFalse(
                                result.getObservations()
                                                .getFirst()
                                                .isSuccess());

                assertTrue(
                                result.getObservations()
                                                .getLast()
                                                .isSuccess());

                assertEquals(
                                AgentTaskStatus.COMPLETED,
                                task(
                                                result,
                                                "task-1")
                                                .getStatus());

                assertEquals(
                                AgentStatus.FINISHED,
                                result.getStatus());
        }

        /*
         * =========================================================
         * Sprint 4 - Runtime Guardrails
         * =========================================================
         */

        @Test
        void shouldFinishWithFallbackWhenMaxStepsIsReached() {

                AgentContext initialized = initializedContext(
                                AgentTask.pending(
                                                "task-1",
                                                "分析第一项问题"),
                                AgentTask.pending(
                                                "task-2",
                                                "分析第二项问题"));

                when(
                                agentPipeline.execute(
                                                any()))
                                .thenReturn(
                                                initialized);

                when(
                                actionSelector.select(
                                                any(),
                                                any()))
                                .thenReturn(
                                                AgentAction.reason(
                                                                "task-1"));

                when(
                                actionExecutionOperator.execute(
                                                any(),
                                                any(),
                                                any()))
                                .thenReturn(
                                                AgentActionExecutionResult.reason(
                                                                "第一项分析完成"));

                when(
                                reflectionService.reflect(
                                                any(),
                                                any()))
                                .thenReturn(
                                                ReflectionResult.of(
                                                                ReflectionDecision.CONTINUE,
                                                                "继续执行后续任务"));

                when(
                                finalAnswerService.generate(
                                                any()))
                                .thenReturn(
                                                "基于当前已有信息生成的最终法律意见");

                AgentContext result = runtime(
                                1,
                                2,
                                2)
                                .run(
                                                AgentContext.from(
                                                                "分析劳动合同"));

                assertEquals(
                                AgentTaskStatus.COMPLETED,
                                task(
                                                result,
                                                "task-1")
                                                .getStatus());

                assertEquals(
                                AgentTaskStatus.PENDING,
                                task(
                                                result,
                                                "task-2")
                                                .getStatus());

                assertEquals(
                                AgentStatus.FINISHED,
                                result.getStatus());

                assertTrue(
                                result.hasFinalAnswer());

                assertEquals(
                                "基于当前已有信息生成的最终法律意见",
                                result.getFinalAnswer());

                assertTrue(
                                result.getExecutionLogs()
                                                .stream()
                                                .anyMatch(
                                                                log -> log.contains(
                                                                                "Maximum execution steps reached: 1")));

                verify(
                                actionExecutionOperator,
                                times(1))
                                .execute(
                                                any(),
                                                any(),
                                                any());

                verify(
                                finalAnswerService,
                                times(1))
                                .generate(
                                                any());
        }

        @Test
        void shouldFinishWhenRetryLimitIsReached() {

                AgentContext initialized = initializedContext(
                                AgentTask.pending(
                                                "task-1",
                                                "分析现有材料"));

                when(
                                agentPipeline.execute(
                                                any()))
                                .thenReturn(
                                                initialized);

                when(
                                actionSelector.select(
                                                any(),
                                                any()))
                                .thenReturn(
                                                AgentAction.reason(
                                                                "task-1"));

                when(
                                actionExecutionOperator.execute(
                                                any(),
                                                any(),
                                                any()))
                                .thenReturn(
                                                AgentActionExecutionResult.reason(
                                                                "分析结果仍然不足"));

                when(
                                reflectionService.reflect(
                                                any(),
                                                any()))
                                .thenReturn(
                                                ReflectionResult.of(
                                                                ReflectionDecision.RETRY,
                                                                "当前结果不足，需要继续分析"));

                when(
                                finalAnswerService.generate(
                                                any()))
                                .thenReturn(
                                                "基于现有信息生成的最终法律意见");

                AgentContext result = runtime(
                                10,
                                2,
                                2)
                                .run(
                                                AgentContext.from(
                                                                "分析劳动合同"));

                /*
                 * 初始执行 + Retry #1 + Retry #2
                 *
                 * 第三次 Reflection 再次返回 RETRY 时，
                 * Runtime Guardrail 终止循环。
                 */
                verify(
                                actionExecutionOperator,
                                times(3))
                                .execute(
                                                any(),
                                                any(),
                                                any());

                verify(
                                reflectionService,
                                times(3))
                                .reflect(
                                                any(),
                                                any());

                assertEquals(
                                AgentStatus.FINISHED,
                                result.getStatus());

                assertTrue(
                                result.hasFinalAnswer());

                assertEquals(
                                "基于现有信息生成的最终法律意见",
                                result.getFinalAnswer());

                assertTrue(
                                result.getExecutionLogs()
                                                .stream()
                                                .anyMatch(
                                                                log -> log.contains(
                                                                                "Maximum retries reached for task: task-1")));

                verify(
                                finalAnswerService,
                                times(1))
                                .generate(
                                                any());
        }

        @Test
        void shouldFinishWhenReplanLimitIsReached() {

                AgentContext initialized = initializedContext(
                                AgentTask.pending(
                                                "task-1",
                                                "初始分析"));

                AgentPlan replannedPlan1 = AgentPlan.from(
                                List.of(
                                                AgentTask.pending(
                                                                "task-2",
                                                                "第一次重新规划")));

                AgentPlan replannedPlan2 = AgentPlan.from(
                                List.of(
                                                AgentTask.pending(
                                                                "task-3",
                                                                "第二次重新规划")));

                when(
                                agentPipeline.execute(
                                                any()))
                                .thenReturn(
                                                initialized);

                /*
                 * 无论当前是 task-1 / task-2 / task-3，
                 * 都生成对应 Task 的 Reason Action。
                 */
                when(
                                actionSelector.select(
                                                any(),
                                                any()))
                                .thenAnswer(
                                                invocation -> {

                                                        AgentTask currentTask = invocation.getArgument(
                                                                        1);

                                                        return AgentAction.reason(
                                                                        currentTask.getId());
                                                });

                when(
                                actionExecutionOperator.execute(
                                                any(),
                                                any(),
                                                any()))
                                .thenReturn(
                                                AgentActionExecutionResult.reason(
                                                                "当前计划仍然不足"));

                when(
                                reflectionService.reflect(
                                                any(),
                                                any()))
                                .thenReturn(
                                                ReflectionResult.of(
                                                                ReflectionDecision.REPLAN,
                                                                "需要重新规划"));

                when(
                                replanningService.replan(
                                                any(),
                                                any()))
                                .thenReturn(
                                                replannedPlan1,
                                                replannedPlan2);

                when(
                                finalAnswerService.generate(
                                                any()))
                                .thenReturn(
                                                "基于已有执行结果生成最终法律意见");

                AgentContext result = runtime(
                                10,
                                2,
                                2)
                                .run(
                                                AgentContext.from(
                                                                "分析劳动合同"));

                /*
                 * maxReplans = 2
                 *
                 * 第一次 REPLAN → Plan 1
                 * 第二次 REPLAN → Plan 2
                 * 第三次要求 REPLAN → Guardrail
                 */
                verify(
                                replanningService,
                                times(2))
                                .replan(
                                                any(),
                                                any());

                /*
                 * task-1
                 * task-2
                 * task-3
                 *
                 * 共执行三个 Action。
                 */
                verify(
                                actionExecutionOperator,
                                times(3))
                                .execute(
                                                any(),
                                                any(),
                                                any());

                verify(
                                reflectionService,
                                times(3))
                                .reflect(
                                                any(),
                                                any());

                assertEquals(
                                AgentStatus.FINISHED,
                                result.getStatus());

                assertTrue(
                                result.hasFinalAnswer());

                assertEquals(
                                "基于已有执行结果生成最终法律意见",
                                result.getFinalAnswer());

                assertTrue(
                                result.getExecutionLogs()
                                                .stream()
                                                .anyMatch(
                                                                log -> log.contains(
                                                                                "Maximum replans reached")));

                verify(
                                finalAnswerService,
                                times(1))
                                .generate(
                                                any());
        }

        @Test
        void shouldNotAllowRetryWhenMaxRetriesPerTaskIsZero() {

                AgentContext initialized = initializedContext(
                                AgentTask.pending(
                                                "task-1",
                                                "分析现有材料"));

                when(
                                agentPipeline.execute(
                                                any()))
                                .thenReturn(
                                                initialized);

                when(
                                actionSelector.select(
                                                any(),
                                                any()))
                                .thenReturn(
                                                AgentAction.reason(
                                                                "task-1"));

                when(
                                actionExecutionOperator.execute(
                                                any(),
                                                any(),
                                                any()))
                                .thenReturn(
                                                AgentActionExecutionResult.reason(
                                                                "当前分析结果不足"));

                when(
                                reflectionService.reflect(
                                                any(),
                                                any()))
                                .thenReturn(
                                                ReflectionResult.of(
                                                                ReflectionDecision.RETRY,
                                                                "需要重新执行"));

                when(
                                finalAnswerService.generate(
                                                any()))
                                .thenReturn(
                                                "当前已有信息的最终答复");

                AgentContext result = runtime(
                                10,
                                0,
                                2)
                                .run(
                                                AgentContext.from(
                                                                "分析劳动合同"));

                verify(
                                actionExecutionOperator,
                                times(1))
                                .execute(
                                                any(),
                                                any(),
                                                any());

                verify(
                                reflectionService,
                                times(1))
                                .reflect(
                                                any(),
                                                any());

                assertEquals(
                                AgentStatus.FINISHED,
                                result.getStatus());

                assertTrue(
                                result.hasFinalAnswer());

                assertTrue(
                                result.getExecutionLogs()
                                                .stream()
                                                .anyMatch(
                                                                log -> log.contains(
                                                                                "Maximum retries reached for task: task-1")));
        }

        @Test
        void shouldNotAllowReplanWhenMaxReplansIsZero() {

                AgentContext initialized = initializedContext(
                                AgentTask.pending(
                                                "task-1",
                                                "分析现有材料"));

                when(
                                agentPipeline.execute(
                                                any()))
                                .thenReturn(
                                                initialized);

                when(
                                actionSelector.select(
                                                any(),
                                                any()))
                                .thenReturn(
                                                AgentAction.reason(
                                                                "task-1"));

                when(
                                actionExecutionOperator.execute(
                                                any(),
                                                any(),
                                                any()))
                                .thenReturn(
                                                AgentActionExecutionResult.reason(
                                                                "现有计划不充分"));

                when(
                                reflectionService.reflect(
                                                any(),
                                                any()))
                                .thenReturn(
                                                ReflectionResult.of(
                                                                ReflectionDecision.REPLAN,
                                                                "需要重新规划"));

                when(
                                finalAnswerService.generate(
                                                any()))
                                .thenReturn(
                                                "当前已有信息的最终答复");

                AgentContext result = runtime(
                                10,
                                2,
                                0)
                                .run(
                                                AgentContext.from(
                                                                "分析劳动合同"));

                verify(
                                replanningService,
                                never())
                                .replan(
                                                any(),
                                                any());

                verify(
                                actionExecutionOperator,
                                times(1))
                                .execute(
                                                any(),
                                                any(),
                                                any());

                assertEquals(
                                AgentStatus.FINISHED,
                                result.getStatus());

                assertTrue(
                                result.hasFinalAnswer());

                assertTrue(
                                result.getExecutionLogs()
                                                .stream()
                                                .anyMatch(
                                                                log -> log.contains(
                                                                                "Maximum replans reached")));
        }

        /*
         * =========================================================
         * Constructor Guardrails
         * =========================================================
         */

        @Test
        void shouldRejectNonPositiveMaxSteps() {

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> runtime(
                                                0,
                                                2,
                                                2));

                assertEquals(
                                "maxSteps must be greater than zero",
                                exception.getMessage());
        }

        @Test
        void shouldRejectNegativeMaxRetriesPerTask() {

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> runtime(
                                                10,
                                                -1,
                                                2));

                assertEquals(
                                "maxRetriesPerTask must not be negative",
                                exception.getMessage());
        }

        @Test
        void shouldRejectNegativeMaxReplans() {

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> runtime(
                                                10,
                                                2,
                                                -1));

                assertEquals(
                                "maxReplans must not be negative",
                                exception.getMessage());
        }

        /*
         * =========================================================
         * Helpers
         * =========================================================
         */

        private DefaultAgentRuntime runtime(
                        int maxSteps) {

                return runtime(
                                maxSteps,
                                2,
                                2);
        }

        private DefaultAgentRuntime runtime(
                        int maxSteps,
                        int maxRetriesPerTask,
                        int maxReplans) {

                return new DefaultAgentRuntime(
                                agentPipeline,
                                actionSelector,
                                actionExecutionOperator,
                                reflectionService,
                                replanningService,
                                finalAnswerService,
                                maxSteps,
                                maxRetriesPerTask,
                                maxReplans);
        }

        private AgentContext initializedContext(
                        AgentTask... tasks) {

                return AgentContext.builder()
                                .goal(
                                                "分析劳动合同")
                                .agentPlan(
                                                AgentPlan.from(
                                                                List.of(
                                                                                tasks)))
                                .status(
                                                AgentStatus.RUNNING)
                                .build();
        }

        private AgentTask task(
                        AgentContext context,
                        String taskId) {

                return context.getAgentPlan()
                                .findTaskById(
                                                taskId)
                                .orElseThrow();
        }
}
