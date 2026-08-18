package com.quince.lawyeraiassistant.security.authentication.jwt;

import com.quince.lawyeraiassistant.security.SecurityTest;
import com.quince.lawyeraiassistant.security.authentication.config.JwtSecurityProperties;
import com.quince.lawyeraiassistant.security.identity.ApplicationUser;
import com.quince.lawyeraiassistant.security.identity.AuthenticationConstants;
import com.quince.lawyeraiassistant.security.identity.UserRole;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SecurityTest
class JwtTokenServiceTest {

    @Test
    void shouldCreateTokenWithIdentityAndTenantClaims() {
        JwtEncoder encoder = mock(JwtEncoder.class);
        Jwt encodedJwt = mock(Jwt.class);
        when(encodedJwt.getTokenValue()).thenReturn("signed-jwt");
        when(encoder.encode(org.mockito.ArgumentMatchers.any())).thenReturn(encodedJwt);

        JwtSecurityProperties properties = new JwtSecurityProperties();
        properties.setIssuer("test-issuer");
        properties.setAccessTokenTtl(Duration.ofMinutes(30));
        JwtTokenService service = new JwtTokenService(encoder, properties);

        String token = service.createAccessToken(new ApplicationUser(
                "user-001",
                "tenant-a",
                "quince",
                "hash",
                Set.of(UserRole.LAWYER, UserRole.ADMIN),
                true));

        ArgumentCaptor<JwtEncoderParameters> parameters =
                ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(encoder).encode(parameters.capture());
        var claims = parameters.getValue().getClaims();

        assertEquals("signed-jwt", token);
        assertEquals("test-issuer", claims.getClaim("iss"));
        assertEquals("user-001", claims.getSubject());
        assertEquals("quince", claims.getClaim("username"));
        assertEquals("tenant-a", claims.getClaim(AuthenticationConstants.CLAIM_TENANT_ID));
        assertEquals(List.of("ADMIN", "LAWYER"), claims.getClaim(AuthenticationConstants.CLAIM_ROLES));
        assertNotNull(claims.getIssuedAt());
        assertEquals(Duration.ofMinutes(30), Duration.between(claims.getIssuedAt(), claims.getExpiresAt()));
    }
}
