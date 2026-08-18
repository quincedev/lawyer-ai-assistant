package com.quince.lawyeraiassistant.security.tenant.quota;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.quince.lawyeraiassistant.security.SecurityTest;
import com.quince.lawyeraiassistant.security.identity.UserRole;
import com.quince.lawyeraiassistant.security.tenant.TenantContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SecurityTest
class InMemoryTenantResourceQuotaServiceTest {

    private TenantResourceQuotaProperties properties;

    private InMemoryTenantResourceQuotaService service;

    @BeforeEach
    void setUp() {

        properties = new TenantResourceQuotaProperties();

        properties.setMaxConcurrentAgentExecutions(
                2);

        service = new InMemoryTenantResourceQuotaService(
                properties);
    }

    @Test
    void shouldAcquireAndReleaseTenantExecutionQuota() {

        TenantContext tenant = tenant(
                "tenant-a",
                "user-001");

        TenantQuotaLease lease = service.acquireAgentExecution(
                tenant);

        assertEquals(
                1,
                service.activeAgentExecutions(
                        "tenant-a"));

        lease.close();

        assertEquals(
                0,
                service.activeAgentExecutions(
                        "tenant-a"));
    }

    @Test
    void shouldRejectExecutionWhenTenantQuotaIsExceeded() {

        TenantContext tenant = tenant(
                "tenant-a",
                "user-001");

        TenantQuotaLease first = service.acquireAgentExecution(
                tenant);

        TenantQuotaLease second = service.acquireAgentExecution(
                tenant);

        assertEquals(
                2,
                service.activeAgentExecutions(
                        "tenant-a"));

        assertThrows(
                TenantResourceQuotaExceededException.class,
                () -> service.acquireAgentExecution(
                        tenant));

        first.close();
        second.close();
    }

    @Test
    void shouldIsolateQuotaAcrossTenants() {

        TenantContext tenantA = tenant(
                "tenant-a",
                "user-a");

        TenantContext tenantB = tenant(
                "tenant-b",
                "user-b");

        TenantQuotaLease a1 = service.acquireAgentExecution(
                tenantA);

        TenantQuotaLease a2 = service.acquireAgentExecution(
                tenantA);

        assertThrows(
                TenantResourceQuotaExceededException.class,
                () -> service.acquireAgentExecution(
                        tenantA));

        /*
         * tenant-a 已满，不应该影响 tenant-b。
         */
        TenantQuotaLease b1 = service.acquireAgentExecution(
                tenantB);

        assertEquals(
                2,
                service.activeAgentExecutions(
                        "tenant-a"));

        assertEquals(
                1,
                service.activeAgentExecutions(
                        "tenant-b"));

        a1.close();
        a2.close();
        b1.close();
    }

    @Test
    void shouldReleaseLeaseOnlyOnce() {

        TenantContext tenant = tenant(
                "tenant-a",
                "user-001");

        TenantQuotaLease lease = service.acquireAgentExecution(
                tenant);

        lease.close();
        lease.close();

        assertEquals(
                0,
                service.activeAgentExecutions(
                        "tenant-a"));
    }

    private TenantContext tenant(
            String tenantId,
            String userId) {

        return new TenantContext(
                tenantId,
                userId,
                userId,
                Set.of(
                        UserRole.LAWYER));
    }
}