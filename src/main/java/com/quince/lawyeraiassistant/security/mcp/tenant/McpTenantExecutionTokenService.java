package com.quince.lawyeraiassistant.security.mcp.tenant;

import com.quince.lawyeraiassistant.security.tenant.TenantContext;

public interface McpTenantExecutionTokenService {

    String issue(
            TenantContext tenantContext);

    TenantContext verify(
            String token);
}