package com.quince.lawyeraiassistant.agent.operator;

import com.quince.lawyeraiassistant.agent.model.AgentAction;
import com.quince.lawyeraiassistant.agent.model.AgentActionExecutionResult;
import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentTask;
import com.quince.lawyeraiassistant.agent.model.ToolAction;
import com.quince.lawyeraiassistant.agent.model.ToolObservation;
import com.quince.lawyeraiassistant.agent.service.AgentFinalAnswerService;
import com.quince.lawyeraiassistant.agent.service.AgentRuntimeReasonService;
import com.quince.lawyeraiassistant.agent.skill.AgentSkill;
import com.quince.lawyeraiassistant.agent.skill.context.SkillContext;
import com.quince.lawyeraiassistant.agent.skill.scope.SkillToolScope;
import com.quince.lawyeraiassistant.agent.tool.ToolActionExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

class DefaultAgentActionExecutionOperatorTest {

        private ToolActionExecutor toolActionExecutor;

        private AgentRuntimeReasonService runtimeReasonService;

        private AgentFinalAnswerService finalAnswerService;

        private DefaultAgentActionExecutionOperator operator;

        private SkillToolScope skillToolScope;

        @BeforeEach
        void setUp() {

                toolActionExecutor = mock(
                                ToolActionExecutor.class);

                runtimeReasonService = mock(
                                AgentRuntimeReasonService.class);

                finalAnswerService = mock(
                                AgentFinalAnswerService.class);

                skillToolScope = new SkillToolScope();

                operator = new DefaultAgentActionExecutionOperator(
                                toolActionExecutor,
                                runtimeReasonService,
                                finalAnswerService,
                                skillToolScope);
        }

        @Test
        void shouldExecuteReasonAction() {

                AgentContext context = AgentContext.from(
                                "分析劳动合同");

                AgentTask task = AgentTask.pending(
                                "task-1",
                                "分析竞业限制条款");

                AgentAction action = AgentAction.reason(
                                "task-1");

                when(
                                runtimeReasonService.reason(
                                                context,
                                                task))
                                .thenReturn(
                                                "该竞业限制条款需要结合补偿约定判断");

                AgentActionExecutionResult result = operator.execute(
                                context,
                                task,
                                action);

                assertTrue(
                                result.isReason());

                assertEquals(
                                "该竞业限制条款需要结合补偿约定判断",
                                result.getContent());

                verify(
                                runtimeReasonService)
                                .reason(
                                                context,
                                                task);

                verify(
                                toolActionExecutor,
                                never())
                                .execute(
                                                org.mockito.ArgumentMatchers.any());

                verify(
                                finalAnswerService,
                                never())
                                .generate(
                                                context);
        }

        @Test
        void shouldExecuteFinalAnswerAction() {

                AgentContext context = AgentContext.from(
                                "分析劳动合同");

                AgentTask task = AgentTask.pending(
                                "task-1",
                                "形成最终意见");

                AgentAction action = AgentAction.finalAnswer(
                                "task-1");

                when(
                                finalAnswerService.generate(
                                                context))
                                .thenReturn(
                                                "该劳动合同存在以下法律风险……");

                AgentActionExecutionResult result = operator.execute(
                                context,
                                task,
                                action);

                assertTrue(
                                result.isFinalAnswer());

                assertEquals(
                                "该劳动合同存在以下法律风险……",
                                result.getContent());

                verify(
                                finalAnswerService)
                                .generate(
                                                context);

                verify(
                                toolActionExecutor,
                                never())
                                .execute(
                                                org.mockito.ArgumentMatchers.any());

                verify(
                                runtimeReasonService,
                                never())
                                .reason(
                                                context,
                                                task);
        }

        @Test
        void shouldRejectMismatchedTaskId() {

                AgentContext context = AgentContext.from(
                                "分析劳动合同");

                AgentTask task = AgentTask.pending(
                                "task-1",
                                "查询法律依据");

                AgentAction action = AgentAction.reason(
                                "task-2");

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> operator.execute(
                                                context,
                                                task,
                                                action));

                assertEquals(
                                "AgentAction taskId must match AgentTask id",
                                exception.getMessage());
        }

        @Test
        void shouldRejectNullContext() {

                AgentTask task = AgentTask.pending(
                                "task-1",
                                "查询法律依据");

                AgentAction action = AgentAction.reason(
                                "task-1");

                assertThrows(
                                NullPointerException.class,
                                () -> operator.execute(
                                                null,
                                                task,
                                                action));
        }

        @Test
        void shouldRejectNullTask() {

                AgentContext context = AgentContext.from(
                                "分析劳动合同");

                AgentAction action = AgentAction.reason(
                                "task-1");

                assertThrows(
                                NullPointerException.class,
                                () -> operator.execute(
                                                context,
                                                null,
                                                action));
        }

        @Test
        void shouldRejectNullAction() {

                AgentContext context = AgentContext.from(
                                "分析劳动合同");

                AgentTask task = AgentTask.pending(
                                "task-1",
                                "查询法律依据");

                assertThrows(
                                NullPointerException.class,
                                () -> operator.execute(
                                                context,
                                                task,
                                                null));
        }

        @Test
        void shouldExecuteToolActionWhenAllowedBySkill() {

                AgentSkill skill = AgentSkill.of(
                                "legal-research",
                                "Legal Research",
                                "用于研究具体法律问题",
                                "执行法律研究",
                                List.of(
                                                "searchLegalKnowledge"),
                                Set.of(
                                                "legal",
                                                "research"));

                AgentContext context = AgentContext.from(
                                "研究劳动合同法律问题")
                                .withSkillContext(
                                                SkillContext.of(
                                                                skill));

                AgentTask task = AgentTask.pending(
                                "task-1",
                                "查询法律依据");

                ToolAction toolAction = ToolAction.of(
                                "task-1",
                                "searchLegalKnowledge");

                AgentAction action = AgentAction.tool(
                                toolAction);

                ToolObservation observation = ToolObservation.success(
                                "task-1",
                                "searchLegalKnowledge",
                                "劳动合同法相关规定");

                when(
                                toolActionExecutor.execute(
                                                toolAction))
                                .thenReturn(
                                                observation);

                AgentActionExecutionResult result = operator.execute(
                                context,
                                task,
                                action);

                assertTrue(
                                result.isTool());

                assertSame(
                                observation,
                                result.getObservation());

                verify(
                                toolActionExecutor)
                                .execute(
                                                toolAction);
        }

        @Test
        void shouldRejectToolActionWhenNotAllowedBySkill() {

                AgentSkill skill = AgentSkill.of(
                                "legal-summary",
                                "Legal Summary",
                                "用于总结已有法律材料",
                                "根据已有上下文进行总结",
                                List.of(),
                                Set.of(
                                                "legal",
                                                "summary"));

                AgentContext context = AgentContext.from(
                                "总结已有法律材料")
                                .withSkillContext(
                                                SkillContext.of(
                                                                skill));

                AgentTask task = AgentTask.pending(
                                "task-1",
                                "总结已有法律材料");

                ToolAction toolAction = ToolAction.of(
                                "task-1",
                                "searchLegalKnowledge");

                AgentAction action = AgentAction.tool(
                                toolAction);

                AgentActionExecutionResult result = operator.execute(
                                context,
                                task,
                                action);

                assertTrue(
                                result.isTool());

                ToolObservation observation = result.getObservation();

                assertTrue(
                                observation.isFailure());

                assertEquals(
                                "task-1",
                                observation.getTaskId());

                assertEquals(
                                "searchLegalKnowledge",
                                observation.getToolName());

                assertEquals(
                                "Tool is not allowed by current Skill: "
                                                + "searchLegalKnowledge",
                                observation.getErrorMessage());

                verify(
                                toolActionExecutor,
                                never())
                                .execute(
                                                org.mockito.ArgumentMatchers.any());
        }

        @Test
        void shouldRejectNullSkillToolScope() {

                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> new DefaultAgentActionExecutionOperator(
                                                toolActionExecutor,
                                                runtimeReasonService,
                                                finalAnswerService,
                                                null));

                assertEquals(
                                "SkillToolScope must not be null",
                                exception.getMessage());
        }
}