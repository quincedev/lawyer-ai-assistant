package com.quince.lawyeraiassistant.agent.runtime.action;

import com.quince.lawyeraiassistant.agent.model.AgentAction;
import com.quince.lawyeraiassistant.agent.model.AgentActionExecutionResult;
import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentTask;

/**
 * Coordinates Agent action selection and execution.
 *
 * <p>
 * Responsibilities:
 * </p>
 *
 * <ul>
 * <li>Prefer deterministic action routing when available.</li>
 * <li>Fallback to the model-based action selector.</li>
 * <li>Apply duplicate tool-call protection.</li>
 * <li>Delegate actual action execution.</li>
 * </ul>
 *
 * <p>
 * Runtime execution-budget guardrails intentionally remain outside this
 * coordinator because {@code DefaultAgentRuntime} must evaluate the final
 * selected action before allowing it to cross the execution boundary.
 * </p>
 */
public interface AgentActionCoordinator {

    AgentAction selectAction(
            AgentContext context,
            AgentTask task);

    AgentActionExecutionResult executeAction(
            AgentContext context,
            AgentTask task,
            AgentAction action);
}