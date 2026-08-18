package com.quince.lawyeraiassistant.agent.tool;

import java.util.Objects;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.security.tenant.TenantContext;

/**
 * Trusted runtime context supplied to Agent Tools.
 *
 * <p>
 * Important:
 * this context is created by Java Runtime,
 * not by the LLM and not by ToolAction arguments.
 * </p>
 */
public final class ToolExecutionContext {

    private final TenantContext tenantContext;

    private ToolExecutionContext(
            TenantContext tenantContext) {

        this.tenantContext = tenantContext;
    }

    public static ToolExecutionContext from(
            AgentContext agentContext) {

        Objects.requireNonNull(
                agentContext,
                "agentContext must not be null");

        return new ToolExecutionContext(
                agentContext.getTenantContext());
    }

    /**
     * Legacy/internal execution without tenant identity.
     *
     * <p>
     * Downstream tenant-aware resources must fail closed
     * to SHARED-only behavior.
     * </p>
     */
    public static ToolExecutionContext sharedOnly() {

        return new ToolExecutionContext(
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
}