package com.quince.lawyeraiassistant.workflow.agent;

import com.quince.lawyeraiassistant.agent.application.AgentApplicationService;
import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentStatus;
import com.quince.lawyeraiassistant.workflow.executor.WorkflowNodeExecutionResult;
import com.quince.lawyeraiassistant.workflow.executor.WorkflowNodeExecutor;
import com.quince.lawyeraiassistant.workflow.model.WorkflowContext;
import com.quince.lawyeraiassistant.workflow.node.WorkflowNode;
import com.quince.lawyeraiassistant.workflow.node.WorkflowNodeType;

import java.util.Map;
import java.util.Objects;

/**
 * 在 Workflow 中执行 Agent Node。
 */
public final class AgentWorkflowNodeExecutor
                implements WorkflowNodeExecutor {

        private final AgentApplicationService agentApplicationService;

        public AgentWorkflowNodeExecutor(
                        AgentApplicationService agentApplicationService) {

                this.agentApplicationService = Objects.requireNonNull(
                                agentApplicationService,
                                "AgentApplicationService must not be null");
        }

        @Override
        public boolean supports(
                        WorkflowNode node) {

                Objects.requireNonNull(
                                node,
                                "WorkflowNode must not be null");

                return node.getType() == WorkflowNodeType.AGENT;
        }

        @Override
        public WorkflowNodeExecutionResult execute(
                        WorkflowNode node,
                        WorkflowContext context) {

                Objects.requireNonNull(
                                node,
                                "WorkflowNode must not be null");

                Objects.requireNonNull(
                                context,
                                "WorkflowContext must not be null");

                String goal = resolveAgentGoal(
                                context);

                AgentContext result = agentApplicationService.execute(
                                goal);

                if (result.getStatus() != AgentStatus.FINISHED) {

                        return WorkflowNodeExecutionResult.failure(
                                        "Agent runtime did not finish successfully");
                }

                if (result.getFinalAnswer() == null
                                || result.getFinalAnswer().isBlank()) {

                        return WorkflowNodeExecutionResult.failure(
                                        "Agent did not produce a final answer");
                }

                return WorkflowNodeExecutionResult.success(
                                Map.of(
                                                AgentWorkflowVariables.AGENT_FINAL_ANSWER,
                                                result.getFinalAnswer()));
        }

        private String resolveAgentGoal(
                        WorkflowContext context) {

                Object value = context.getVariable(
                                AgentWorkflowVariables.AGENT_GOAL);

                if (!(value instanceof String goal)
                                || goal.isBlank()) {

                        throw new IllegalStateException(
                                        "Agent goal is missing from WorkflowContext");
                }

                return goal.trim();
        }
}