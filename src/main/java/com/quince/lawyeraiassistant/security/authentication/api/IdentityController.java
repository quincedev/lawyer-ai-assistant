package com.quince.lawyeraiassistant.security.authentication.api;

import java.util.Set;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.quince.lawyeraiassistant.security.identity.UserRole;
import com.quince.lawyeraiassistant.security.tenant.TenantContext;
import com.quince.lawyeraiassistant.security.tenant.TenantContextProvider;

@RestController
@RequestMapping("/api/auth")
public class IdentityController {

    private final TenantContextProvider tenantContextProvider;

    public IdentityController(
            TenantContextProvider tenantContextProvider) {

        this.tenantContextProvider = tenantContextProvider;
    }

    @GetMapping("/me")
    public CurrentIdentityResponse me() {

        TenantContext context = tenantContextProvider.current();

        return new CurrentIdentityResponse(
                context.userId(),
                context.username(),
                context.tenantId(),
                context.roles());
    }

    public record CurrentIdentityResponse(
            String userId,
            String username,
            String tenantId,
            Set<UserRole> roles) {
    }
}