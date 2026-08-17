package com.quince.lawyeraiassistant.workflow.agent;

import com.quince.lawyeraiassistant.agent.application.AgentApplicationService;
import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentStatus;
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentWorkflowNodeExecutorTest {

        private AgentApplicationService agentApplicationService;

        private AgentWorkflowNodeExecutor executor;

        @BeforeEach
        void setUp() {

                agentApplicationService = mock(
                                AgentApplicationService.class);

                executor = new AgentWorkflowNodeExecutor(
                                agentApplicationService);
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
        void shouldExecuteAgentApplicationServiceWithWorkflowGoal() {

                WorkflowNode node = WorkflowNode.agent(
                                "legal-agent",
                                "Legal Agent",
                                "执行法律分析");

                String goal = "研究违法解除劳动合同的法律责任";

                WorkflowContext workflowContext = createWorkflowContext(
                                goal);

                AgentContext agentResult = AgentContext.from(
                                goal)
                                .toBuilder()
                                .status(
                                                AgentStatus.FINISHED)
                                .finalAnswer(
                                                "违法解除劳动合同应依法承担赔偿责任。")
                                .build();

                when(
                                agentApplicationService.execute(
                                                goal))
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

                verify(
                                agentApplicationService)
                                .execute(
                                                goal);
        }

        @Test
        void shouldTrimAgentGoalBeforeApplicationServiceExecution() {

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
                                agentApplicationService.execute(
                                                "分析劳动合同法律风险"))
                                .thenReturn(
                                                agentResult);

                executor.execute(
                                node,
                                workflowContext);

                ArgumentCaptor<String> captor = ArgumentCaptor.forClass(
                                String.class);

                verify(
                                agentApplicationService)
                                .execute(
                                                captor.capture());

                assertEquals(
                                "分析劳动合同法律风险",
                                captor.getValue());
        }

        @Test
        void shouldFailWhenAgentApplicationServiceDoesNotFinish() {

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
                                agentApplicationService.execute(
                                                "分析劳动合同"))
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
                                agentApplicationService.execute(
                                                "分析劳动合同"))
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
                                agentApplicationService.execute(
                                                "分析劳动合同"))
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
        void shouldRejectNullAgentApplicationService() {

                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> new AgentWorkflowNodeExecutor(
                                                null));

                assertEquals(
                                "AgentApplicationService must not be null",
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