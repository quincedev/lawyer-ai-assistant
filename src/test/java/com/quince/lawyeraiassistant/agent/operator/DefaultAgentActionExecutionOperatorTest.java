package com.quince.lawyeraiassistant.agent.operator;

import com.quince.lawyeraiassistant.agent.model.AgentAction;
import com.quince.lawyeraiassistant.agent.model.AgentActionExecutionResult;
import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentTask;
import com.quince.lawyeraiassistant.agent.model.ToolAction;
import com.quince.lawyeraiassistant.agent.model.ToolObservation;
import com.quince.lawyeraiassistant.agent.service.AgentFinalAnswerService;
import com.quince.lawyeraiassistant.agent.service.AgentRuntimeReasonService;
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

class DefaultAgentActionExecutionOperatorTest {

    private ToolActionExecutor toolActionExecutor;

    private AgentRuntimeReasonService runtimeReasonService;

    private AgentFinalAnswerService finalAnswerService;

    private DefaultAgentActionExecutionOperator operator;

    @BeforeEach
    void setUp() {

        toolActionExecutor = mock(
                ToolActionExecutor.class);

        runtimeReasonService = mock(
                AgentRuntimeReasonService.class);

        finalAnswerService = mock(
                AgentFinalAnswerService.class);

        operator = new DefaultAgentActionExecutionOperator(
                toolActionExecutor,
                runtimeReasonService,
                finalAnswerService);
    }

    @Test
    void shouldExecuteToolAction() {

        AgentContext context = AgentContext.from(
                "分析劳动合同");

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

        verify(
                runtimeReasonService,
                never())
                .reason(
                        context,
                        task);

        verify(
                finalAnswerService,
                never())
                .generate(
                        context);
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
}