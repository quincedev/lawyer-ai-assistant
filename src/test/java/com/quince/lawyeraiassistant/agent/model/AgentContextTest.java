package com.quince.lawyeraiassistant.agent.model;

import com.quince.lawyeraiassistant.agent.skill.AgentSkill;
import com.quince.lawyeraiassistant.agent.skill.context.SkillContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentContextTest {

        @Test
        void initialContextShouldNotHaveSkill() {
                AgentContext context = AgentContext.from(
                                "研究劳动合同问题");

                assertFalse(context.hasSkill());
                assertTrue(context.getSkillContext().isEmpty());
                assertTrue(context.getSelectedSkill().isEmpty());
        }

        @Test
        void shouldAttachSkillContextWithoutMutatingOriginalContext() {
                AgentContext original = AgentContext.from(
                                "研究劳动合同问题");
                SkillContext skillContext = SkillContext.of(
                                createSkill());

                AgentContext updated = original.withSkillContext(
                                skillContext);

                assertFalse(original.hasSkill());
                assertTrue(updated.hasSkill());
                assertSame(skillContext, updated.getSkillContext().orElseThrow());
                assertEquals(
                                "legal-research",
                                updated.getSelectedSkill().orElseThrow().getId());
        }

        @Test
        void skillContextShouldSurviveContextEvolution() {
                AgentContext context = AgentContext.from(
                                                "研究劳动合同问题")
                                .withSkillContext(
                                                SkillContext.of(createSkill()));

                AgentContext updated = context
                                .withReasonResult(ReasonResult.from("需要先检索法律依据"))
                                .appendExecutionLog("Reason completed");

                assertTrue(updated.hasSkill());
                assertEquals(
                                "legal-research",
                                updated.getSelectedSkill().orElseThrow().getId());
        }

        @Test
        void shouldRejectNullSkillContext() {
                AgentContext context = AgentContext.from(
                                "研究劳动合同问题");

                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> context.withSkillContext(null));

                assertEquals(
                                "SkillContext must not be null",
                                exception.getMessage());
        }

        private AgentSkill createSkill() {
                return AgentSkill.of(
                                "legal-research",
                                "Legal Research",
                                "用于研究具体法律问题",
                                "执行法律研究",
                                List.of("searchLegalKnowledge"),
                                Set.of("legal", "research"));
        }

        @Test
        void shouldCreateInitialContextFromGoal() {
                AgentContext context = AgentContext.from(
                                "分析劳动合同并生成律师意见书");

                assertEquals(
                                "分析劳动合同并生成律师意见书",
                                context.getGoal());

                assertEquals(
                                AgentStatus.CREATED,
                                context.getStatus());

                /*
                 * Reason 尚未执行，因此 ReasonResult 为 null。
                 */
                assertNull(
                                context.getReasonResult());

                assertFalse(
                                context.hasReasonResult());

                /*
                 * AgentPlan 使用空对象设计，不使用 null。
                 */
                assertNotNull(
                                context.getAgentPlan());

                assertFalse(
                                context.hasAgentPlan());

                assertFalse(
                                context.getAgentPlan()
                                                .hasTasks());

                assertEquals(
                                0,
                                context.getAgentPlan()
                                                .taskCount());

                assertTrue(
                                context.getExecutionLogs()
                                                .isEmpty());

                assertFalse(
                                context.isRunning());

                assertFalse(
                                context.isFinished());

                assertFalse(
                                context.isFailed());

                assertFalse(
                                context.hasExecutionLogs());

                assertEquals(
                                0,
                                context.executionLogCount());
                assertNotNull(
                                context.getObservations());

                assertFalse(
                                context.hasObservations());

                assertEquals(
                                0,
                                context.observationCount());
        }

        @Test
        void shouldTrimGoal() {
                AgentContext context = AgentContext.from(
                                "  分析劳动合同  ");

                assertEquals(
                                "分析劳动合同",
                                context.getGoal());
        }

        @Test
        void shouldDefaultNullStatusToCreated() {
                AgentContext context = AgentContext.builder()
                                .goal("测试目标")
                                .status(null)
                                .build();

                assertEquals(
                                AgentStatus.CREATED,
                                context.getStatus());
        }

        @Test
        void shouldNormalizeNullLogsToEmptyList() {
                AgentContext context = AgentContext.builder()
                                .goal("测试目标")
                                .executionLogs(null)
                                .build();

                assertNotNull(
                                context.getExecutionLogs());

                assertTrue(
                                context.getExecutionLogs()
                                                .isEmpty());
        }

        @Test
        void shouldNormalizeNullAgentPlanToEmptyPlan() {
                AgentContext context = AgentContext.builder()
                                .goal("测试目标")
                                .agentPlan(null)
                                .build();

                assertNotNull(
                                context.getAgentPlan());

                assertFalse(
                                context.hasAgentPlan());

                assertTrue(
                                context.getAgentPlan()
                                                .getTasks()
                                                .isEmpty());
        }

        @Test
        void shouldCreateRunningContextWithToBuilder() {
                AgentContext originalContext = AgentContext.from(
                                "分析劳动合同");

                AgentContext runningContext = originalContext.toBuilder()
                                .status(
                                                AgentStatus.RUNNING)
                                .build();

                assertNotSame(
                                originalContext,
                                runningContext);

                assertEquals(
                                AgentStatus.CREATED,
                                originalContext.getStatus());

                assertEquals(
                                AgentStatus.RUNNING,
                                runningContext.getStatus());

                assertTrue(
                                runningContext.isRunning());

                assertEquals(
                                originalContext.getGoal(),
                                runningContext.getGoal());

                assertSame(
                                originalContext.getAgentPlan(),
                                runningContext.getAgentPlan());
        }

        @Test
        void shouldAddReasonResultWithoutModifyingOriginalContext() {
                AgentContext originalContext = AgentContext.from(
                                "分析劳动合同");

                ReasonResult reasonResult = ReasonResult.from(
                                "用户希望分析劳动合同并识别法律风险");

                AgentContext updatedContext = originalContext.withReasonResult(
                                reasonResult);

                assertNotSame(
                                originalContext,
                                updatedContext);

                assertNull(
                                originalContext.getReasonResult());

                assertFalse(
                                originalContext.hasReasonResult());

                assertSame(
                                reasonResult,
                                updatedContext.getReasonResult());

                assertTrue(
                                updatedContext.hasReasonResult());

                assertEquals(
                                originalContext.getGoal(),
                                updatedContext.getGoal());

                assertEquals(
                                originalContext.getStatus(),
                                updatedContext.getStatus());

                assertSame(
                                originalContext.getAgentPlan(),
                                updatedContext.getAgentPlan());

                assertEquals(
                                originalContext.getExecutionLogs(),
                                updatedContext.getExecutionLogs());
        }

        @Test
        void shouldRejectNullReasonResultWhenUsingHelperMethod() {
                AgentContext context = AgentContext.from(
                                "分析劳动合同");

                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> context.withReasonResult(null));

                assertEquals(
                                "ReasonResult must not be null",
                                exception.getMessage());
        }

        @Test
        void shouldAddAgentPlanWithoutModifyingOriginalContext() {
                AgentContext originalContext = AgentContext.from(
                                "分析劳动合同");

                AgentPlan agentPlan = AgentPlan.from(
                                List.of(
                                                AgentTask.pending(
                                                                "task-1",
                                                                "读取劳动合同"),
                                                AgentTask.pending(
                                                                "task-2",
                                                                "识别法律风险")));

                AgentContext updatedContext = originalContext.withAgentPlan(
                                agentPlan);

                assertNotSame(
                                originalContext,
                                updatedContext);

                /*
                 * 原始 Context 仍然保持空计划。
                 */
                assertFalse(
                                originalContext.hasAgentPlan());

                assertNotNull(
                                originalContext.getAgentPlan());

                assertTrue(
                                originalContext.getAgentPlan()
                                                .getTasks()
                                                .isEmpty());

                /*
                 * 新 Context 包含真正的 Planning 结果。
                 */
                assertTrue(
                                updatedContext.hasAgentPlan());

                assertSame(
                                agentPlan,
                                updatedContext.getAgentPlan());

                assertEquals(
                                2,
                                updatedContext.getAgentPlan()
                                                .taskCount());

                assertEquals(
                                originalContext.getGoal(),
                                updatedContext.getGoal());

                assertEquals(
                                originalContext.getStatus(),
                                updatedContext.getStatus());

                assertEquals(
                                originalContext.getReasonResult(),
                                updatedContext.getReasonResult());

                assertEquals(
                                originalContext.getExecutionLogs(),
                                updatedContext.getExecutionLogs());
        }

        @Test
        void shouldRejectNullAgentPlanWhenUsingHelperMethod() {
                AgentContext context = AgentContext.from(
                                "分析劳动合同");

                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> context.withAgentPlan(null));

                assertEquals(
                                "AgentPlan must not be null",
                                exception.getMessage());
        }

        @Test
        void shouldTreatEmptyAgentPlanAsNoPlan() {
                AgentContext context = AgentContext.from(
                                "分析劳动合同")
                                .withAgentPlan(
                                                AgentPlan.empty());

                assertNotNull(
                                context.getAgentPlan());

                assertFalse(
                                context.hasAgentPlan());

                assertEquals(
                                0,
                                context.getAgentPlan()
                                                .taskCount());
        }

        @Test
        void shouldPreserveReasonResultWhenAddingAgentPlan() {
                ReasonResult reasonResult = ReasonResult.from(
                                "用户希望生成律师意见书");

                AgentPlan agentPlan = AgentPlan.from(
                                List.of(
                                                AgentTask.pending(
                                                                "task-1",
                                                                "读取劳动合同")));

                AgentContext context = AgentContext.from(
                                "分析劳动合同")
                                .withReasonResult(
                                                reasonResult);

                AgentContext updatedContext = context.withAgentPlan(
                                agentPlan);

                assertSame(
                                reasonResult,
                                updatedContext.getReasonResult());

                assertSame(
                                agentPlan,
                                updatedContext.getAgentPlan());

                assertTrue(
                                updatedContext.hasReasonResult());

                assertTrue(
                                updatedContext.hasAgentPlan());
        }

        @Test
        void shouldPreserveAgentPlanWhenAddingReasonResult() {
                AgentPlan agentPlan = AgentPlan.from(
                                List.of(
                                                AgentTask.pending(
                                                                "task-1",
                                                                "读取劳动合同")));

                ReasonResult reasonResult = ReasonResult.from(
                                "用户希望分析劳动合同");

                AgentContext context = AgentContext.from(
                                "分析劳动合同")
                                .withAgentPlan(
                                                agentPlan);

                AgentContext updatedContext = context.withReasonResult(
                                reasonResult);

                assertSame(
                                agentPlan,
                                updatedContext.getAgentPlan());

                assertSame(
                                reasonResult,
                                updatedContext.getReasonResult());
        }

        @Test
        void shouldPreserveReasonResultAndAgentPlanWhenAppendingLog() {
                ReasonResult reasonResult = ReasonResult.from(
                                "用户希望生成律师意见书");

                AgentPlan agentPlan = AgentPlan.from(
                                List.of(
                                                AgentTask.pending(
                                                                "task-1",
                                                                "读取劳动合同")));

                AgentContext context = AgentContext.from(
                                "分析劳动合同")
                                .withReasonResult(
                                                reasonResult)
                                .withAgentPlan(
                                                agentPlan);

                AgentContext updatedContext = context.appendExecutionLog(
                                "Planning completed");

                assertSame(
                                reasonResult,
                                updatedContext.getReasonResult());

                assertSame(
                                agentPlan,
                                updatedContext.getAgentPlan());

                assertEquals(
                                List.of(
                                                "Planning completed"),
                                updatedContext.getExecutionLogs());
        }

        @Test
        void shouldAppendExecutionLogWithoutModifyingOriginal() {
                AgentContext originalContext = AgentContext.from(
                                "分析劳动合同");

                AgentContext updatedContext = originalContext.appendExecutionLog(
                                "  Reason completed  ");

                assertNotSame(
                                originalContext,
                                updatedContext);

                assertTrue(
                                originalContext.getExecutionLogs()
                                                .isEmpty());

                assertEquals(
                                List.of(
                                                "Reason completed"),
                                updatedContext.getExecutionLogs());

                assertTrue(
                                updatedContext.hasExecutionLogs());

                assertEquals(
                                1,
                                updatedContext.executionLogCount());
        }

        @Test
        void shouldPreserveExistingLogsWhenAppending() {
                AgentContext context = AgentContext.builder()
                                .goal("生成律师意见书")
                                .executionLogs(
                                                List.of(
                                                                "Reason completed"))
                                .build();

                AgentContext result = context.appendExecutionLog(
                                "Planning completed");

                assertEquals(
                                List.of(
                                                "Reason completed",
                                                "Planning completed"),
                                result.getExecutionLogs());
        }

        @Test
        void shouldCreateDefensiveCopyOfExecutionLogs() {
                List<String> mutableLogs = new ArrayList<>();

                mutableLogs.add(
                                "Reason completed");

                AgentContext context = AgentContext.builder()
                                .goal("测试目标")
                                .executionLogs(
                                                mutableLogs)
                                .build();

                mutableLogs.clear();

                assertEquals(
                                List.of(
                                                "Reason completed"),
                                context.getExecutionLogs());
        }

        @Test
        void shouldExposeUnmodifiableExecutionLogs() {
                AgentContext context = AgentContext.builder()
                                .goal("测试目标")
                                .executionLogs(
                                                List.of(
                                                                "Reason completed"))
                                .build();

                assertThrows(
                                UnsupportedOperationException.class,
                                () -> context.getExecutionLogs()
                                                .add(
                                                                "Illegal log"));
        }

        @Test
        void shouldIdentifyFinishedStatus() {
                AgentContext context = AgentContext.builder()
                                .goal("测试目标")
                                .status(
                                                AgentStatus.FINISHED)
                                .build();

                assertTrue(
                                context.isFinished());

                assertFalse(
                                context.isRunning());

                assertFalse(
                                context.isFailed());
        }

        @Test
        void shouldIdentifyFailedStatus() {
                AgentContext context = AgentContext.builder()
                                .goal("测试目标")
                                .status(
                                                AgentStatus.FAILED)
                                .build();

                assertTrue(
                                context.isFailed());

                assertFalse(
                                context.isRunning());

                assertFalse(
                                context.isFinished());
        }

        @Test
        void shouldRejectNullGoal() {
                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> AgentContext.from(null));

                assertEquals(
                                "Goal must not be null",
                                exception.getMessage());
        }

        @Test
        void shouldRejectBlankGoal() {
                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> AgentContext.from("   "));

                assertEquals(
                                "Goal must not be blank",
                                exception.getMessage());
        }

        @Test
        void shouldRejectNullExecutionLog() {
                AgentContext context = AgentContext.from(
                                "测试目标");

                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> context.appendExecutionLog(
                                                null));

                assertEquals(
                                "Execution log must not be null",
                                exception.getMessage());
        }

        @Test
        void shouldRejectBlankExecutionLog() {
                AgentContext context = AgentContext.from(
                                "测试目标");

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> context.appendExecutionLog(
                                                "   "));

                assertEquals(
                                "Execution log must not be blank",
                                exception.getMessage());
        }

        @Test
        void shouldRejectNullElementInExecutionLogs() {
                List<String> logs = new ArrayList<>();

                logs.add(
                                "Reason completed");

                logs.add(null);

                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> AgentContext.builder()
                                                .goal("测试目标")
                                                .executionLogs(logs)
                                                .build());

                assertEquals(
                                "Execution log must not be null",
                                exception.getMessage());
        }

        @Test
        void shouldSupportEqualsAndHashCode() {
                AgentContext first = AgentContext.builder()
                                .goal("分析劳动合同")
                                .reasonResult(
                                                ReasonResult.from(
                                                                "用户希望分析劳动合同"))
                                .agentPlan(
                                                AgentPlan.from(
                                                                List.of(
                                                                                AgentTask.pending(
                                                                                                "task-1",
                                                                                                "读取劳动合同"))))
                                .status(
                                                AgentStatus.RUNNING)
                                .executionLogs(
                                                List.of(
                                                                "Reason completed",
                                                                "Planning completed"))
                                .build();

                AgentContext second = AgentContext.builder()
                                .goal("分析劳动合同")
                                .reasonResult(
                                                ReasonResult.from(
                                                                "用户希望分析劳动合同"))
                                .agentPlan(
                                                AgentPlan.from(
                                                                List.of(
                                                                                AgentTask.pending(
                                                                                                "task-1",
                                                                                                "读取劳动合同"))))
                                .status(
                                                AgentStatus.RUNNING)
                                .executionLogs(
                                                List.of(
                                                                "Reason completed",
                                                                "Planning completed"))
                                .build();

                assertEquals(
                                first,
                                second);

                assertEquals(
                                first.hashCode(),
                                second.hashCode());
        }

        @Test
        void shouldNormalizeNullObservationsToEmptyList() {

                AgentContext context = AgentContext.builder()
                                .goal("测试目标")
                                .observations(null)
                                .build();

                assertNotNull(
                                context.getObservations());

                assertTrue(
                                context.getObservations()
                                                .isEmpty());

                assertFalse(
                                context.hasObservations());

                assertEquals(
                                0,
                                context.observationCount());
        }

        @Test
        void shouldAppendObservationWithoutModifyingOriginalContext() {

                AgentContext originalContext = AgentContext.from(
                                "分析劳动合同");

                ToolObservation observation = ToolObservation.success(
                                "task-1",
                                "searchLegalKnowledge",
                                "检索到劳动合同法相关规定");

                AgentContext updatedContext = originalContext.appendObservation(
                                observation);

                assertNotSame(
                                originalContext,
                                updatedContext);

                assertFalse(
                                originalContext.hasObservations());

                assertTrue(
                                originalContext.getObservations()
                                                .isEmpty());

                assertTrue(
                                updatedContext.hasObservations());

                assertEquals(
                                1,
                                updatedContext.observationCount());

                assertSame(
                                observation,
                                updatedContext.getObservations()
                                                .getFirst());
        }

        @Test
        void shouldPreserveExistingObservationsWhenAppending() {

                ToolObservation firstObservation = ToolObservation.success(
                                "task-1",
                                "readDocument",
                                "合同读取成功");

                ToolObservation secondObservation = ToolObservation.success(
                                "task-2",
                                "searchLegalKnowledge",
                                "法律检索成功");

                AgentContext context = AgentContext.builder()
                                .goal("分析劳动合同")
                                .observations(
                                                List.of(
                                                                firstObservation))
                                .build();

                AgentContext result = context.appendObservation(
                                secondObservation);

                assertEquals(
                                List.of(
                                                firstObservation,
                                                secondObservation),
                                result.getObservations());
        }

        @Test
        void shouldCreateDefensiveCopyOfObservations() {

                List<ToolObservation> mutableObservations = new ArrayList<>();

                mutableObservations.add(
                                ToolObservation.success(
                                                "task-1",
                                                "readDocument",
                                                "合同读取成功"));

                AgentContext context = AgentContext.builder()
                                .goal("分析劳动合同")
                                .observations(
                                                mutableObservations)
                                .build();

                mutableObservations.clear();

                assertEquals(
                                1,
                                context.observationCount());
        }

        @Test
        void shouldExposeUnmodifiableObservations() {

                AgentContext context = AgentContext.builder()
                                .goal("分析劳动合同")
                                .observations(
                                                List.of(
                                                                ToolObservation.success(
                                                                                "task-1",
                                                                                "readDocument",
                                                                                "合同读取成功")))
                                .build();

                assertThrows(
                                UnsupportedOperationException.class,
                                () -> context.getObservations()
                                                .add(
                                                                ToolObservation.success(
                                                                                "task-2",
                                                                                "searchLegalKnowledge",
                                                                                "法律检索成功")));
        }

        @Test
        void shouldRejectNullObservationWhenAppending() {

                AgentContext context = AgentContext.from(
                                "分析劳动合同");

                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> context.appendObservation(
                                                null));

                assertEquals(
                                "ToolObservation must not be null",
                                exception.getMessage());
        }

        @Test
        void shouldRejectNullElementInObservations() {

                List<ToolObservation> observations = new ArrayList<>();

                observations.add(
                                ToolObservation.success(
                                                "task-1",
                                                "readDocument",
                                                "合同读取成功"));

                observations.add(null);

                assertThrows(
                                NullPointerException.class,
                                () -> AgentContext.builder()
                                                .goal("分析劳动合同")
                                                .observations(observations)
                                                .build());
        }

        @Test
        void shouldAppendRuntimeReasonObservationWithoutModifyingOriginalContext() {

                AgentContext originalContext = AgentContext.from(
                                "分析劳动合同");

                RuntimeReasonObservation observation = RuntimeReasonObservation.of(
                                "task-1",
                                "现有材料不足以确认解除是否合法");

                AgentContext updatedContext = originalContext.appendRuntimeReasonObservation(
                                observation);

                assertNotSame(
                                originalContext,
                                updatedContext);

                assertTrue(
                                originalContext.getRuntimeReasonObservations()
                                                .isEmpty());

                assertEquals(
                                List.of(
                                                observation),
                                updatedContext.getRuntimeReasonObservations());
        }

        @Test
        void shouldRejectNullRuntimeReasonObservationWhenAppending() {

                AgentContext context = AgentContext.from(
                                "分析劳动合同");

                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> context.appendRuntimeReasonObservation(
                                                null));

                assertEquals(
                                "RuntimeReasonObservation must not be null",
                                exception.getMessage());
        }
}
