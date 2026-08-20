package com.quince.lawyeraiassistant.agent.application;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.stream.AgentStreamPublisher;
import com.quince.lawyeraiassistant.security.tenant.TenantContext;

/**
 * Application boundary for executing Agent requests.
 *
 * <p>
 * External adapters such as REST Controllers,
 * Workflow Nodes or future Production APIs
 * should execute Agent requests through this interface,
 * instead of invoking AgentRuntime directly.
 * </p>
 */
public interface AgentApplicationService {

        /**
         * Executes an Agent request for the given goal.
         *
         * @param goal user/application goal
         * @return final AgentContext
         */
        AgentContext execute(
                        String goal);

        AgentContext executeStreaming(
                        String goal,
                        AgentStreamPublisher publisher);

        AgentContext executeStreaming(
                        String goal,
                        TenantContext tenantContext,
                        AgentStreamPublisher publisher);
}