package com.quince.lawyeraiassistant.security.tenant.quota;

import com.quince.lawyeraiassistant.security.tenant.TenantContext;

public interface TenantResourceQuotaService {

    TenantQuotaLease acquireAgentExecution(
            TenantContext tenantContext);

    int activeAgentExecutions(
            String tenantId);
}