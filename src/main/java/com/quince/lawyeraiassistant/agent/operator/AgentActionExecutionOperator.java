package com.quince.lawyeraiassistant.agent.operator;

import com.quince.lawyeraiassistant.agent.model.AgentAction;
import com.quince.lawyeraiassistant.agent.model.AgentActionExecutionResult;
import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentTask;

public interface AgentActionExecutionOperator {

    AgentActionExecutionResult execute(
            AgentContext context,
            AgentTask task,
            AgentAction action);
}