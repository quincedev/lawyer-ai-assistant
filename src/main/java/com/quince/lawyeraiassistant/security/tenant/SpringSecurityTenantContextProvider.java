package com.quince.lawyeraiassistant.security.tenant;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.quince.lawyeraiassistant.security.identity.AuthenticationConstants;
import com.quince.lawyeraiassistant.security.identity.UserRole;

@Component
public final class SpringSecurityTenantContextProvider
        implements TenantContextProvider {

    @Override
    public TenantContext current() {

        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {

            throw new IllegalStateException(
                    "Authenticated JWT is required");
        }

        Jwt jwt = jwtAuthentication.getToken();

        String tenantId = jwt.getClaimAsString(
                AuthenticationConstants.CLAIM_TENANT_ID);

        String username = jwt.getClaimAsString(
                "username");

        List<String> roleClaims = jwt.getClaimAsStringList(
                AuthenticationConstants.CLAIM_ROLES);

        if (roleClaims == null
                || roleClaims.isEmpty()) {

            throw new IllegalStateException(
                    "JWT roles claim is missing");
        }

        Set<UserRole> roles = roleClaims
                .stream()
                .map(
                        UserRole::valueOf)
                .collect(
                        Collectors.toUnmodifiableSet());

        return new TenantContext(
                tenantId,
                jwt.getSubject(),
                username,
                roles);
    }
}