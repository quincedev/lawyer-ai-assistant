package com.quince.lawyeraiassistant.agent.service;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentPlan;
import com.quince.lawyeraiassistant.agent.model.ReflectionResult;

public interface AgentReplanningService {

    AgentPlan replan(
            AgentContext context,
            ReflectionResult reflectionResult);
}