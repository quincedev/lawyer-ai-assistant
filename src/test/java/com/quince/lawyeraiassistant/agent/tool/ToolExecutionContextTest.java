package com.quince.lawyeraiassistant.agent.tool;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.security.SecurityTest;
import com.quince.lawyeraiassistant.security.identity.UserRole;
import com.quince.lawyeraiassistant.security.tenant.TenantContext;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SecurityTest
class ToolExecutionContextTest {

    @Test
    void shouldCarryTrustedExecutionGoalFromAgentContext() {

        ToolExecutionContext executionContext = ToolExecutionContext.from(
                AgentContext.builder()
                        .goal("  分析劳动合同违法解除的主要法律责任  ")
                        .tenantContext(tenantA())
                        .build());

        assertTrue(executionContext.hasExecutionGoal());
        assertEquals(
                "分析劳动合同违法解除的主要法律责任",
                executionContext.requireExecutionGoal());
    }

    @Test
    void sharedOnlyShouldNotExposeExecutionGoal() {

        ToolExecutionContext executionContext = ToolExecutionContext.sharedOnly();

        assertFalse(executionContext.hasExecutionGoal());
        assertNull(executionContext.getExecutionGoal());
        assertThrows(
                IllegalStateException.class,
                executionContext::requireExecutionGoal);
    }

    @Test
    void shouldPropagateTenantContextFromAgentContext() {
        TenantContext tenant = tenantA();
        AgentContext agentContext = AgentContext.builder()
                .goal("research")
                .tenantContext(tenant)
                .build();

        ToolExecutionContext executionContext = ToolExecutionContext.from(agentContext);

        assertSame(tenant, executionContext.requireTenantContext());
    }

    @Test
    void sharedOnlyShouldHaveNoTenantContext() {
        assertFalse(ToolExecutionContext.sharedOnly().hasTenantContext());
    }

    @Test
    void sharedOnlyShouldFailWhenTenantIsRequired() {
        assertThrows(
                IllegalStateException.class,
                () -> ToolExecutionContext.sharedOnly().requireTenantContext());
    }

    private TenantContext tenantA() {
        return new TenantContext(
                "tenant-a",
                "user-a",
                "lawyer-a",
                Set.of(UserRole.LAWYER));
    }
}
