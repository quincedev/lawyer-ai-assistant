package com.quince.lawyeraiassistant.agent.runtime.policy;

import com.quince.lawyeraiassistant.security.runtime.AgentExecutionBudget;
import com.quince.lawyeraiassistant.security.runtime.RuntimeGuardrailOperation;
import com.quince.lawyeraiassistant.security.runtime.RuntimeGuardrailResult;
import com.quince.lawyeraiassistant.security.runtime.resource.RuntimeResourceResult;
import com.quince.lawyeraiassistant.security.runtime.resource.RuntimeResourceType;

/**
 * Runtime execution policy boundary.
 *
 * <p>
 * Centralizes execution-budget and runtime-resource guardrail decisions
 * so that {@code DefaultAgentRuntime} only orchestrates the Agent loop.
 * </p>
 */
public interface AgentExecutionPolicyFacade {

    /**
     * Creates a fresh execution budget.
     *
     * <p>
     * One budget belongs to exactly one Agent execution.
     * </p>
     */
    AgentExecutionBudget createBudget();

    /**
     * Evaluates an execution-count/time based runtime guardrail.
     *
     * <p>
     * Denied decisions are audited by the policy facade.
     * </p>
     */
    RuntimeGuardrailResult evaluateRuntime(
            RuntimeGuardrailOperation operation,
            AgentExecutionBudget budget);

    /**
     * Evaluates a runtime resource guardrail.
     *
     * <p>
     * Denied decisions are audited by the policy facade.
     * </p>
     */
    RuntimeResourceResult evaluateResource(
            RuntimeResourceType resourceType,
            int actualLength,
            String taskId);
}