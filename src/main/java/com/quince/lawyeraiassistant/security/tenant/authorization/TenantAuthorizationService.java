package com.quince.lawyeraiassistant.security.tenant.authorization;

import com.quince.lawyeraiassistant.security.tenant.TenantContext;

public interface TenantAuthorizationService {

    void authorizeAgentAccess(
            TenantContext context);
}