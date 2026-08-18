package com.quince.lawyeraiassistant.security.tenant;

import com.quince.lawyeraiassistant.security.SecurityTest;
import com.quince.lawyeraiassistant.security.identity.AuthenticationConstants;
import com.quince.lawyeraiassistant.security.identity.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SecurityTest
class SpringSecurityTenantContextProviderTest {

    private final SpringSecurityTenantContextProvider provider =
            new SpringSecurityTenantContextProvider();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldBuildTenantContextFromJwt() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user-001")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .claim("username", "quince")
                .claim(AuthenticationConstants.CLAIM_TENANT_ID, "tenant-a")
                .claim(AuthenticationConstants.CLAIM_ROLES, List.of("LAWYER"))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt));

        TenantContext context = provider.current();

        assertEquals("tenant-a", context.tenantId());
        assertEquals("user-001", context.userId());
        assertEquals("quince", context.username());
        assertEquals(Set.of(UserRole.LAWYER), context.roles());
    }

    @Test
    void shouldRejectRequestWithoutJwtAuthentication() {
        assertThrows(IllegalStateException.class, provider::current);
    }
}
