package com.quince.lawyeraiassistant.agent.operator;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentPlan;
import com.quince.lawyeraiassistant.agent.model.AgentStatus;
import com.quince.lawyeraiassistant.agent.model.AgentTask;
import com.quince.lawyeraiassistant.agent.model.ReasonResult;
import com.quince.lawyeraiassistant.agent.prompt.model.PlanningPromptContext;
import com.quince.lawyeraiassistant.agent.service.AgentPlanningService;
import com.quince.lawyeraiassistant.agent.skill.AgentSkill;
import com.quince.lawyeraiassistant.agent.skill.context.SkillContext;
import com.quince.lawyeraiassistant.agent.skill.scope.SkillToolScope;
import com.quince.lawyeraiassistant.agent.tool.AgentToolRegistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SpringAiPlanningOperatorTest {

        private AgentPlanningService planningService;

        private SpringAiPlanningOperator operator;

        private SkillToolScope skillToolScope;

        private AgentToolRegistry toolRegistry;

        @BeforeEach
        void setUp() {

                planningService = mock(
                                AgentPlanningService.class);

                skillToolScope = mock(
                                SkillToolScope.class);

                toolRegistry = mock(
                                AgentToolRegistry.class);

                when(
                                toolRegistry.names())
                                .thenReturn(
                                                List.of());

                when(
                                skillToolScope.filterAllowed(
                                                any(),
                                                any()))
                                .thenReturn(
                                                List.of());

                operator = new SpringAiPlanningOperator(
                                planningService,
                                skillToolScope,
                                toolRegistry);
        }

        @Test
        void shouldGenerateAgentPlanAndUpdateContext() {

                AgentContext originalContext = AgentContext.from(
                                "分析劳动合同")
                                .withReasonResult(
                                                ReasonResult.from(
                                                                "用户希望分析劳动合同。"));

                AgentPlan plan = AgentPlan.from(
                                List.of(
                                                AgentTask.pending(
                                                                "task-1",
                                                                "读取劳动合同")));

                when(
                                planningService.plan(
                                                any(PlanningPromptContext.class)))
                                .thenReturn(plan);

                AgentContext result = operator.execute(
                                originalContext);

                assertNotSame(
                                originalContext,
                                result);

                assertTrue(
                                result.hasAgentPlan());

                assertSame(
                                plan,
                                result.getAgentPlan());

                assertEquals(
                                AgentStatus.RUNNING,
                                result.getStatus());

                assertEquals(
                                List.of(
                                                "Planning completed"),
                                result.getExecutionLogs());

                /*
                 * 原 Context 不变
                 */
                assertFalse(
                                originalContext.hasAgentPlan());

                assertTrue(
                                originalContext
                                                .getExecutionLogs()
                                                .isEmpty());
        }

        @Test
        void shouldPassGoalAndReasonResultToPlanningService() {

                ReasonResult reason = ReasonResult.from(
                                "分析劳动合同。");

                AgentContext context = AgentContext.from(
                                "分析劳动合同")
                                .withReasonResult(
                                                reason);

                when(
                                planningService.plan(
                                                any()))
                                .thenReturn(
                                                AgentPlan.empty());

                operator.execute(
                                context);

                ArgumentCaptor<PlanningPromptContext> captor = ArgumentCaptor.forClass(
                                PlanningPromptContext.class);

                verify(
                                planningService).plan(
                                                captor.capture());

                PlanningPromptContext promptContext = captor.getValue();

                assertEquals(
                                "分析劳动合同",
                                promptContext.getGoal());

                assertSame(
                                reason,
                                promptContext.getReasonResult());
        }

        @Test
        void shouldPreserveReasonResultAfterPlanning() {

                ReasonResult reason = ReasonResult.from(
                                "分析劳动合同。");

                AgentPlan plan = AgentPlan.from(
                                List.of(
                                                AgentTask.pending(
                                                                "task-1",
                                                                "读取劳动合同")));

                AgentContext context = AgentContext.from(
                                "分析劳动合同")
                                .withReasonResult(
                                                reason);

                when(
                                planningService.plan(
                                                any()))
                                .thenReturn(
                                                plan);

                AgentContext result = operator.execute(
                                context);

                assertSame(
                                reason,
                                result.getReasonResult());

                assertSame(
                                plan,
                                result.getAgentPlan());
        }

        @Test
        void shouldAppendPlanningCompletedLog() {

                AgentContext context = AgentContext.from(
                                "分析劳动合同")
                                .withReasonResult(
                                                ReasonResult.from(
                                                                "分析劳动合同。"))
                                .appendExecutionLog(
                                                "Reason completed");

                when(
                                planningService.plan(
                                                any()))
                                .thenReturn(
                                                AgentPlan.empty());

                AgentContext result = operator.execute(
                                context);

                assertEquals(
                                List.of(
                                                "Reason completed",
                                                "Planning completed"),
                                result.getExecutionLogs());
        }

        @Test
        void shouldRejectNullContext() {

                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> operator.execute(null));

                assertEquals(
                                "AgentContext must not be null",
                                exception.getMessage());

                verify(
                                planningService,
                                never()).plan(
                                                any());
        }

        @Test
        void shouldRejectContextWithoutReasonResult() {

                AgentContext context = AgentContext.from(
                                "分析劳动合同");

                IllegalStateException exception = assertThrows(
                                IllegalStateException.class,
                                () -> operator.execute(
                                                context));

                assertEquals(
                                "ReasonResult must exist before planning",
                                exception.getMessage());

                verify(
                                planningService,
                                never()).plan(
                                                any());
        }

        @Test
        void shouldRejectNullPlanReturnedByService() {

                AgentContext context = AgentContext.from(
                                "分析劳动合同")
                                .withReasonResult(
                                                ReasonResult.from(
                                                                "分析劳动合同。"));

                when(
                                planningService.plan(
                                                any()))
                                .thenReturn(
                                                null);

                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> operator.execute(
                                                context));

                assertEquals(
                                "AgentPlanningService must not return null",
                                exception.getMessage());
        }

        @Test
        void shouldPropagatePlanningException() {

                AgentContext context = AgentContext.from(
                                "分析劳动合同")
                                .withReasonResult(
                                                ReasonResult.from(
                                                                "分析劳动合同。"));

                IllegalStateException expected = new IllegalStateException(
                                "Planning failed");

                when(
                                planningService.plan(
                                                any()))
                                .thenThrow(
                                                expected);

                IllegalStateException actual = assertThrows(
                                IllegalStateException.class,
                                () -> operator.execute(
                                                context));

                assertSame(
                                expected,
                                actual);
        }

        @Test
        void shouldRejectNullPlanningService() {

                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> new SpringAiPlanningOperator(
                                                null,
                                                skillToolScope,
                                                toolRegistry));

                assertEquals(
                                "agentPlanningService must not be null",
                                exception.getMessage());
        }

        @Test
        void shouldPassSkillInstructionsAndAvailableToolsToPlanningService() {

                AgentSkill skill = AgentSkill.of(
                                "legal-research",
                                "Legal Research",
                                "用于研究具体法律问题",
                                "优先检索法律知识库，并基于检索结果完成法律分析",
                                List.of(
                                                "searchLegalKnowledge"),
                                Set.of(
                                                "legal",
                                                "research"));

                SkillContext skillContext = SkillContext.of(
                                skill);

                AgentContext context = AgentContext.from(
                                "分析违法解除劳动合同")
                                .withReasonResult(
                                                ReasonResult.from(
                                                                "需要分析违法解除的法律依据"))
                                .withSkillContext(
                                                skillContext);

                when(
                                toolRegistry.names())
                                .thenReturn(
                                                List.of(
                                                                "searchLegalKnowledge"));

                when(
                                skillToolScope.filterAllowed(
                                                context.getSkillContext(),
                                                List.of(
                                                                "searchLegalKnowledge")))
                                .thenReturn(
                                                List.of(
                                                                "searchLegalKnowledge"));

                when(
                                planningService.plan(
                                                any(
                                                                PlanningPromptContext.class)))
                                .thenReturn(
                                                AgentPlan.empty());

                operator.execute(
                                context);

                ArgumentCaptor<PlanningPromptContext> captor = ArgumentCaptor.forClass(
                                PlanningPromptContext.class);

                verify(
                                planningService)
                                .plan(
                                                captor.capture());

                PlanningPromptContext promptContext = captor.getValue();

                assertEquals(
                                "优先检索法律知识库，并基于检索结果完成法律分析",
                                promptContext.getSkillInstructions());

                assertEquals(
                                "searchLegalKnowledge",
                                promptContext.getAvailableTools());
        }

        @Test
        void shouldUseNoneSkillAndNoToolsWhenNoSkillSelected() {

                AgentContext context = AgentContext.from(
                                "分析普通问题")
                                .withReasonResult(
                                                ReasonResult.from(
                                                                "这是一个普通问题"));

                when(
                                toolRegistry.names())
                                .thenReturn(
                                                List.of(
                                                                "searchLegalKnowledge"));

                when(
                                skillToolScope.filterAllowed(
                                                context.getSkillContext(),
                                                List.of(
                                                                "searchLegalKnowledge")))
                                .thenReturn(
                                                List.of());

                when(
                                planningService.plan(
                                                any(
                                                                PlanningPromptContext.class)))
                                .thenReturn(
                                                AgentPlan.empty());

                operator.execute(
                                context);

                ArgumentCaptor<PlanningPromptContext> captor = ArgumentCaptor.forClass(
                                PlanningPromptContext.class);

                verify(
                                planningService)
                                .plan(
                                                captor.capture());

                PlanningPromptContext promptContext = captor.getValue();

                assertEquals(
                                "无",
                                promptContext.getSkillInstructions());

                assertEquals(
                                "无",
                                promptContext.getAvailableTools());
        }

        @Test
        void shouldRejectNullSkillToolScope() {

                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> new SpringAiPlanningOperator(
                                                planningService,
                                                null,
                                                toolRegistry));

                assertEquals(
                                "skillToolScope must not be null",
                                exception.getMessage());
        }

        @Test
        void shouldRejectNullToolRegistry() {

                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> new SpringAiPlanningOperator(
                                                planningService,
                                                skillToolScope,
                                                null));

                assertEquals(
                                "toolRegistry must not be null",
                                exception.getMessage());
        }
}