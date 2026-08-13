package com.quince.lawyeraiassistant.workflow.service;

import com.quince.lawyeraiassistant.workflow.agent.AgentWorkflowVariables;
import com.quince.lawyeraiassistant.workflow.executor.WorkflowExecutor;
import com.quince.lawyeraiassistant.workflow.model.WorkflowContext;
import com.quince.lawyeraiassistant.workflow.model.WorkflowDefinition;
import com.quince.lawyeraiassistant.workflow.node.WorkflowNode;
import com.quince.lawyeraiassistant.workflow.transition.WorkflowTransition;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class LegalAgentWorkflowService {

    private final WorkflowExecutor workflowExecutor;

    public LegalAgentWorkflowService(
            WorkflowExecutor workflowExecutor) {

        this.workflowExecutor = Objects.requireNonNull(
                workflowExecutor,
                "WorkflowExecutor must not be null");
    }

    public WorkflowContext execute(
            String goal) {

        Objects.requireNonNull(
                goal,
                "Goal must not be null");

        if (goal.isBlank()) {
            throw new IllegalArgumentException(
                    "Goal must not be blank");
        }

        WorkflowDefinition definition = createDefinition();

        WorkflowContext context = WorkflowContext.pending(
                definition)
                .mergeVariables(
                        Map.of(
                                AgentWorkflowVariables.AGENT_GOAL,
                                goal.trim()));

        return workflowExecutor.execute(
                definition,
                context);
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
                "生成 Workflow 最终结果");

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