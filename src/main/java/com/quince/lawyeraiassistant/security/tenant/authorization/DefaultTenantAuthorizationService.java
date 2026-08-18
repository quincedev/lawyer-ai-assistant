package com.quince.lawyeraiassistant.security.tenant.authorization;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.quince.lawyeraiassistant.security.identity.UserRole;
import com.quince.lawyeraiassistant.security.tenant.TenantContext;

@Service
public final class DefaultTenantAuthorizationService
        implements TenantAuthorizationService {

    private static final Set<UserRole> AGENT_ALLOWED_ROLES = EnumSet.of(
            UserRole.LAWYER,
            UserRole.DIRECTOR,
            UserRole.ADMIN);

    @Override
    public void authorizeAgentAccess(
            TenantContext context) {

        Objects.requireNonNull(
                context,
                "TenantContext must not be null");

        boolean allowed = context.roles()
                .stream()
                .anyMatch(
                        AGENT_ALLOWED_ROLES::contains);

        if (!allowed) {

            throw new TenantAccessDeniedException();
        }
    }
}