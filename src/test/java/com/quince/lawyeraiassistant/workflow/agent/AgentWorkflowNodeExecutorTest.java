package com.quince.lawyeraiassistant.workflow.agent;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentStatus;
import com.quince.lawyeraiassistant.agent.runtime.AgentRuntime;
import com.quince.lawyeraiassistant.workflow.executor.WorkflowNodeExecutionResult;
import com.quince.lawyeraiassistant.workflow.model.WorkflowContext;
import com.quince.lawyeraiassistant.workflow.model.WorkflowStatus;
import com.quince.lawyeraiassistant.workflow.node.WorkflowNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentWorkflowNodeExecutorTest {

    private AgentRuntime agentRuntime;

    private AgentWorkflowNodeExecutor executor;

    @BeforeEach
    void setUp() {

        agentRuntime = mock(
                AgentRuntime.class);

        executor = new AgentWorkflowNodeExecutor(
                agentRuntime);
    }

    @Test
    void shouldSupportAgentNode() {

        WorkflowNode node = WorkflowNode.agent(
                "legal-agent",
                "Legal Agent",
                "执行法律分析");

        assertTrue(
                executor.supports(
                        node));
    }

    @Test
    void shouldNotSupportStandardNode() {

        WorkflowNode node = WorkflowNode.of(
                "prepare-request",
                "Prepare Request",
                "准备请求");

        assertFalse(
                executor.supports(
                        node));
    }

    @Test
    void shouldExecuteAgentRuntimeWithWorkflowGoal() {

        WorkflowNode node = WorkflowNode.agent(
                "legal-agent",
                "Legal Agent",
                "执行法律分析");

        WorkflowContext workflowContext = createWorkflowContext(
                "研究违法解除劳动合同的法律责任");

        AgentContext agentResult = AgentContext.from(
                "研究违法解除劳动合同的法律责任")
                .toBuilder()
                .status(
                        AgentStatus.FINISHED)
                .finalAnswer(
                        "违法解除劳动合同应依法承担赔偿责任。")
                .build();

        when(
                agentRuntime.run(
                        any(
                                AgentContext.class)))
                .thenReturn(
                        agentResult);

        WorkflowNodeExecutionResult result = executor.execute(
                node,
                workflowContext);

        assertTrue(
                result.isSuccess());

        assertEquals(
                "违法解除劳动合同应依法承担赔偿责任。",
                result.getVariables()
                        .get(
                                AgentWorkflowVariables.AGENT_FINAL_ANSWER));

        ArgumentCaptor<AgentContext> captor = ArgumentCaptor.forClass(
                AgentContext.class);

        verify(
                agentRuntime)
                .run(
                        captor.capture());

        AgentContext input = captor.getValue();

        assertEquals(
                "研究违法解除劳动合同的法律责任",
                input.getGoal());
    }

    @Test
    void shouldTrimAgentGoalBeforeRuntimeExecution() {

        WorkflowNode node = WorkflowNode.agent(
                "legal-agent",
                "Legal Agent",
                null);

        WorkflowContext workflowContext = createWorkflowContext(
                "   分析劳动合同法律风险   ");

        AgentContext agentResult = AgentContext.from(
                "分析劳动合同法律风险")
                .toBuilder()
                .status(
                        AgentStatus.FINISHED)
                .finalAnswer(
                        "存在一定法律风险。")
                .build();

        when(
                agentRuntime.run(
                        any(
                                AgentContext.class)))
                .thenReturn(
                        agentResult);

        executor.execute(
                node,
                workflowContext);

        ArgumentCaptor<AgentContext> captor = ArgumentCaptor.forClass(
                AgentContext.class);

        verify(
                agentRuntime)
                .run(
                        captor.capture());

        assertEquals(
                "分析劳动合同法律风险",
                captor.getValue()
                        .getGoal());
    }

    @Test
    void shouldFailWhenAgentRuntimeDoesNotFinish() {

        WorkflowNode node = WorkflowNode.agent(
                "legal-agent",
                "Legal Agent",
                null);

        WorkflowContext workflowContext = createWorkflowContext(
                "分析劳动合同");

        AgentContext agentResult = AgentContext.from(
                "分析劳动合同")
                .toBuilder()
                .status(
                        AgentStatus.FAILED)
                .build();

        when(
                agentRuntime.run(
                        any(
                                AgentContext.class)))
                .thenReturn(
                        agentResult);

        WorkflowNodeExecutionResult result = executor.execute(
                node,
                workflowContext);

        assertFalse(
                result.isSuccess());

        assertEquals(
                "Agent runtime did not finish successfully",
                result.getErrorMessage());
    }

    @Test
    void shouldFailWhenAgentFinalAnswerIsMissing() {

        WorkflowNode node = WorkflowNode.agent(
                "legal-agent",
                "Legal Agent",
                null);

        WorkflowContext workflowContext = createWorkflowContext(
                "分析劳动合同");

        AgentContext agentResult = AgentContext.from(
                "分析劳动合同")
                .toBuilder()
                .status(
                        AgentStatus.FINISHED)
                .finalAnswer(
                        null)
                .build();

        when(
                agentRuntime.run(
                        any(
                                AgentContext.class)))
                .thenReturn(
                        agentResult);

        WorkflowNodeExecutionResult result = executor.execute(
                node,
                workflowContext);

        assertFalse(
                result.isSuccess());

        assertEquals(
                "Agent did not produce a final answer",
                result.getErrorMessage());
    }

    @Test
    @Disabled("Blank final answers are rejected by AgentContext before executor invocation")
    void shouldFailWhenAgentFinalAnswerIsBlank() {

        WorkflowNode node = WorkflowNode.agent(
                "legal-agent",
                "Legal Agent",
                null);

        WorkflowContext workflowContext = createWorkflowContext(
                "分析劳动合同");

        AgentContext agentResult = AgentContext.from(
                "分析劳动合同")
                .toBuilder()
                .status(
                        AgentStatus.FINISHED)
                .finalAnswer(
                        "   ")
                .build();

        when(
                agentRuntime.run(
                        any(
                                AgentContext.class)))
                .thenReturn(
                        agentResult);

        WorkflowNodeExecutionResult result = executor.execute(
                node,
                workflowContext);

        assertFalse(
                result.isSuccess());

        assertEquals(
                "Agent did not produce a final answer",
                result.getErrorMessage());
    }

    @Test
    void shouldRejectMissingAgentGoal() {

        WorkflowNode node = WorkflowNode.agent(
                "legal-agent",
                "Legal Agent",
                null);

        WorkflowContext context = WorkflowContext.builder()
                .workflowId(
                        "legal-agent-workflow")
                .currentNodeId(
                        "legal-agent")
                .status(
                        WorkflowStatus.RUNNING)
                .variables(
                        Map.of())
                .nodeStatuses(
                        Map.of())
                .errorMessage(
                        "")
                .build();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> executor.execute(
                        node,
                        context));

        assertEquals(
                "Agent goal is missing from WorkflowContext",
                exception.getMessage());
    }

    @Test
    void shouldRejectBlankAgentGoal() {

        WorkflowNode node = WorkflowNode.agent(
                "legal-agent",
                "Legal Agent",
                null);

        WorkflowContext context = createWorkflowContext(
                "   ");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> executor.execute(
                        node,
                        context));

        assertEquals(
                "Agent goal is missing from WorkflowContext",
                exception.getMessage());
    }

    @Test
    void shouldRejectNullNode() {

        WorkflowContext context = createWorkflowContext(
                "分析劳动合同");

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> executor.execute(
                        null,
                        context));

        assertEquals(
                "WorkflowNode must not be null",
                exception.getMessage());
    }

    @Test
    void shouldRejectNullContext() {

        WorkflowNode node = WorkflowNode.agent(
                "legal-agent",
                "Legal Agent",
                null);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> executor.execute(
                        node,
                        null));

        assertEquals(
                "WorkflowContext must not be null",
                exception.getMessage());
    }

    @Test
    void shouldRejectNullAgentRuntime() {

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new AgentWorkflowNodeExecutor(
                        null));

        assertEquals(
                "AgentRuntime must not be null",
                exception.getMessage());
    }

    private WorkflowContext createWorkflowContext(
            String goal) {

        return WorkflowContext.builder()
                .workflowId(
                        "legal-agent-workflow")
                .currentNodeId(
                        "legal-agent")
                .status(
                        WorkflowStatus.RUNNING)
                .variables(
                        Map.of(
                                AgentWorkflowVariables.AGENT_GOAL,
                                goal))
                .nodeStatuses(
                        Map.of())
                .errorMessage(
                        "")
                .build();
    }
}
