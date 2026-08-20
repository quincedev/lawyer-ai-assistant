package com.quince.lawyeraiassistant.agent.tool;

import java.util.Objects;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.security.tenant.TenantContext;

public final class ToolExecutionContext {

    private final TenantContext tenantContext;

    /*
     * Runtime trusted original goal.
     *
     * 注意：
     * 这是 Java Runtime 从 AgentContext 中获取，
     * 不是 LLM-generated Tool arguments。
     */
    private final String executionGoal;

    private ToolExecutionContext(
            TenantContext tenantContext,
            String executionGoal) {

        this.tenantContext = tenantContext;

        this.executionGoal = normalizeOptionalText(
                executionGoal);
    }

    public static ToolExecutionContext from(
            AgentContext agentContext) {

        Objects.requireNonNull(
                agentContext,
                "agentContext must not be null");

        return new ToolExecutionContext(
                agentContext.getTenantContext(),
                agentContext.getGoal());
    }

    public static ToolExecutionContext sharedOnly() {

        return new ToolExecutionContext(
                null,
                null);
    }

    public boolean hasTenantContext() {

        return tenantContext != null;
    }

    public TenantContext requireTenantContext() {

        if (tenantContext == null) {

            throw new IllegalStateException(
                    "TenantContext is required for tenant-aware tool execution");
        }

        return tenantContext;
    }

    public boolean hasExecutionGoal() {

        return executionGoal != null;
    }

    public String requireExecutionGoal() {

        if (executionGoal == null) {

            throw new IllegalStateException(
                    "Execution goal is required");
        }

        return executionGoal;
    }

    public String getExecutionGoal() {

        return executionGoal;
    }

    private static String normalizeOptionalText(
            String value) {

        if (value == null) {

            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}