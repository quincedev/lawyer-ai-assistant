package com.quince.lawyeraiassistant.workflow.agent;

import com.quince.lawyeraiassistant.agent.application.AgentApplicationService;
import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentStatus;
import com.quince.lawyeraiassistant.workflow.executor.DefaultWorkflowExecutor;
import com.quince.lawyeraiassistant.workflow.executor.WorkflowExecutor;
import com.quince.lawyeraiassistant.workflow.model.WorkflowContext;
import com.quince.lawyeraiassistant.workflow.model.WorkflowDefinition;
import com.quince.lawyeraiassistant.workflow.model.WorkflowNodeStatus;
import com.quince.lawyeraiassistant.workflow.model.WorkflowStatus;
import com.quince.lawyeraiassistant.workflow.node.WorkflowNode;
import com.quince.lawyeraiassistant.workflow.node.executor.GenerateResultWorkflowNodeExecutor;
import com.quince.lawyeraiassistant.workflow.node.executor.PrepareRequestWorkflowNodeExecutor;
import com.quince.lawyeraiassistant.workflow.transition.WorkflowTransition;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentWorkflowIntegrationTest {

        @Test
        void shouldExecuteStandardAgentStandardWorkflow() {

                AgentApplicationService agentApplicationService = mock(
                                AgentApplicationService.class);

                WorkflowDefinition definition = createDefinition();

                String goal = "研究违法解除劳动合同需要承担什么法律责任";

                WorkflowContext context = WorkflowContext.pending(
                                definition)
                                .mergeVariables(
                                                Map.of(
                                                                AgentWorkflowVariables.AGENT_GOAL,
                                                                goal));

                AgentContext agentResult = AgentContext.from(
                                goal)
                                .toBuilder()
                                .status(
                                                AgentStatus.FINISHED)
                                .finalAnswer(
                                                "用人单位违法解除劳动合同的，应依法承担赔偿责任。")
                                .build();

                when(
                                agentApplicationService.execute(
                                                goal))
                                .thenReturn(
                                                agentResult);

                WorkflowExecutor workflowExecutor = new DefaultWorkflowExecutor(
                                List.of(
                                                new PrepareRequestWorkflowNodeExecutor(),
                                                new AgentWorkflowNodeExecutor(
                                                                agentApplicationService),
                                                new GenerateResultWorkflowNodeExecutor()));

                WorkflowContext result = workflowExecutor.execute(
                                definition,
                                context);

                assertEquals(
                                WorkflowStatus.COMPLETED,
                                result.getStatus());

                assertEquals(
                                WorkflowNodeStatus.COMPLETED,
                                result.getNodeStatus(
                                                "prepare-request"));

                assertEquals(
                                WorkflowNodeStatus.COMPLETED,
                                result.getNodeStatus(
                                                "legal-agent"));

                assertEquals(
                                WorkflowNodeStatus.COMPLETED,
                                result.getNodeStatus(
                                                "generate-result"));

                assertEquals(
                                "用人单位违法解除劳动合同的，应依法承担赔偿责任。",
                                result.getVariable(
                                                AgentWorkflowVariables.AGENT_FINAL_ANSWER));

                assertEquals(
                                "用人单位违法解除劳动合同的，应依法承担赔偿责任。",
                                result.getVariable(
                                                "workflowResult"));

                verify(
                                agentApplicationService)
                                .execute(
                                                goal);
        }

        @Test
        void shouldFailWorkflowWhenAgentRuntimeFails() {

                AgentApplicationService agentApplicationService = mock(
                                AgentApplicationService.class);

                WorkflowDefinition definition = createDefinition();

                String goal = "分析劳动合同";

                WorkflowContext context = WorkflowContext.pending(
                                definition)
                                .mergeVariables(
                                                Map.of(
                                                                AgentWorkflowVariables.AGENT_GOAL,
                                                                goal));

                AgentContext agentResult = AgentContext.from(
                                goal)
                                .toBuilder()
                                .status(
                                                AgentStatus.FAILED)
                                .build();

                when(
                                agentApplicationService.execute(
                                                goal))
                                .thenReturn(
                                                agentResult);

                WorkflowExecutor workflowExecutor = new DefaultWorkflowExecutor(
                                List.of(
                                                new PrepareRequestWorkflowNodeExecutor(),
                                                new AgentWorkflowNodeExecutor(
                                                                agentApplicationService),
                                                new GenerateResultWorkflowNodeExecutor()));

                WorkflowContext result = workflowExecutor.execute(
                                definition,
                                context);

                assertEquals(
                                WorkflowStatus.FAILED,
                                result.getStatus());

                assertEquals(
                                WorkflowNodeStatus.COMPLETED,
                                result.getNodeStatus(
                                                "prepare-request"));

                assertEquals(
                                WorkflowNodeStatus.FAILED,
                                result.getNodeStatus(
                                                "legal-agent"));

                assertEquals(
                                WorkflowNodeStatus.PENDING,
                                result.getNodeStatus(
                                                "generate-result"));

                assertEquals(
                                "Agent runtime did not finish successfully",
                                result.getErrorMessage());
        }

        @Test
        void shouldFailWorkflowWhenAgentGoalIsMissing() {

                AgentApplicationService agentApplicationService = mock(
                                AgentApplicationService.class);

                WorkflowDefinition definition = createDefinition();

                WorkflowContext context = WorkflowContext.pending(
                                definition);

                WorkflowExecutor workflowExecutor = new DefaultWorkflowExecutor(
                                List.of(
                                                new PrepareRequestWorkflowNodeExecutor(),
                                                new AgentWorkflowNodeExecutor(
                                                                agentApplicationService),
                                                new GenerateResultWorkflowNodeExecutor()));

                IllegalStateException exception = assertThrows(
                                IllegalStateException.class,
                                () -> workflowExecutor.execute(
                                                definition,
                                                context));

                assertEquals(
                                "Agent goal is missing from WorkflowContext",
                                exception.getMessage());
        }

        private WorkflowDefinition createDefinition() {

                WorkflowNode prepareRequest = WorkflowNode.of(
                                "prepare-request",
                                "Prepare Request",
                                "准备法律分析请求");

                WorkflowNode legalAgent = WorkflowNode.agent(
                                "legal-agent",
                                "Legal Agent",
                                "通过 Agent Runtime 执行法律分析");

                WorkflowNode generateResult = WorkflowNode.of(
                                "generate-result",
                                "Generate Result",
                                "生成最终结果");

                return WorkflowDefinition.of(
                                "legal-agent-workflow",
                                "Legal Agent Workflow",
                                "prepare-request",
                                List.of(
                                                prepareRequest,
                                                legalAgent,
                                                generateResult),
                                List.of(
                                                WorkflowTransition.of(
                                                                "prepare-request",
                                                                "legal-agent"),
                                                WorkflowTransition.of(
                                                                "legal-agent",
                                                                "generate-result")));
        }
}