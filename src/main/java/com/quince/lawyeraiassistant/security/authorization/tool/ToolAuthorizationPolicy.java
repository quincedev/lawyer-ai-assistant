package com.quince.lawyeraiassistant.security.authorization.tool;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.ToolAction;

/**
 * Tool Authorization Policy SPI.
 *
 * <p>
 * A policy evaluates whether a proposed ToolAction
 * is allowed under the current Agent execution context.
 * </p>
 *
 * <p>
 * The LLM only proposes an action.
 * The application authorization layer determines
 * whether that action may actually execute.
 * </p>
 */
public interface ToolAuthorizationPolicy {

    /**
     * Unique policy name.
     */
    String name();

    /**
     * Authorizes a proposed ToolAction.
     *
     * @param context current Agent execution context
     * @param action  proposed Tool action
     * @return authorization result
     */
    ToolAuthorizationResult authorize(
            AgentContext context,
            ToolAction action);
}