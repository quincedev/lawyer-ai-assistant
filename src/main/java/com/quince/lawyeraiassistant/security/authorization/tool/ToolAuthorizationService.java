package com.quince.lawyeraiassistant.security.authorization.tool;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.ToolAction;

/**
 * Central Tool Authorization boundary.
 */
public interface ToolAuthorizationService {

    ToolAuthorizationResult authorize(
            AgentContext context,
            ToolAction action);
}