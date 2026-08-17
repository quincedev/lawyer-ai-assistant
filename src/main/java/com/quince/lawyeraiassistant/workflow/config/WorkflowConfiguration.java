package com.quince.lawyeraiassistant.workflow.config;

import com.quince.lawyeraiassistant.agent.application.AgentApplicationService;
import com.quince.lawyeraiassistant.workflow.agent.AgentWorkflowNodeExecutor;
import com.quince.lawyeraiassistant.workflow.executor.DefaultWorkflowExecutor;
import com.quince.lawyeraiassistant.workflow.executor.WorkflowExecutor;
import com.quince.lawyeraiassistant.workflow.executor.WorkflowNodeExecutor;
import com.quince.lawyeraiassistant.workflow.node.executor.GenerateResultWorkflowNodeExecutor;
import com.quince.lawyeraiassistant.workflow.node.executor.PrepareRequestWorkflowNodeExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class WorkflowConfiguration {

    @Bean
    public AgentWorkflowNodeExecutor agentWorkflowNodeExecutor(
            AgentApplicationService agentApplicationService) {

        return new AgentWorkflowNodeExecutor(
                agentApplicationService);
    }

    @Bean
    public PrepareRequestWorkflowNodeExecutor prepareRequestWorkflowNodeExecutor() {

        return new PrepareRequestWorkflowNodeExecutor();
    }

    @Bean
    public GenerateResultWorkflowNodeExecutor generateResultWorkflowNodeExecutor() {

        return new GenerateResultWorkflowNodeExecutor();
    }

    @Bean
    public WorkflowExecutor workflowExecutor(
            List<WorkflowNodeExecutor> nodeExecutors) {

        return new DefaultWorkflowExecutor(
                nodeExecutors);
    }
}