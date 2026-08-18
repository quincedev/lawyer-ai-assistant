package com.quince.lawyeraiassistant.security.mcp.tenant;

import com.quince.lawyeraiassistant.security.SecurityTest;
import com.quince.lawyeraiassistant.security.identity.AuthenticationConstants;
import com.quince.lawyeraiassistant.security.identity.UserRole;
import com.quince.lawyeraiassistant.security.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SecurityTest
class DefaultMcpTenantExecutionTokenServiceTest {

    @Test
    void shouldIssueAndVerifyTenant() {
        DefaultMcpTenantExecutionTokenService service = service(secret("tenant-token-secret-key-aaaaaaaa"));

        TenantContext verified = service.verify(service.issue(tenantA()));

        assertEquals("tenant-a", verified.tenantId());
        assertEquals("user-a", verified.userId());
        assertEquals(Set.of(UserRole.LAWYER), verified.roles());
    }

    @Test
    void shouldRejectTokenSignedWithDifferentKey() {
        String token = service(secret("tenant-token-secret-key-aaaaaaaa")).issue(tenantA());

        assertThrows(
                JwtException.class,
                () -> service(secret("tenant-token-secret-key-bbbbbbbb")).verify(token));
    }

    @Test
    void shouldRejectWrongPurpose() {
        SecretKey key = secret("tenant-token-secret-key-aaaaaaaa");
        String token = encode(key, claims(Instant.now(), "access-token"));

        assertThrows(
                IllegalArgumentException.class,
                () -> service(key).verify(token));
    }

    @Test
    void shouldRejectExpiredToken() {
        SecretKey key = secret("tenant-token-secret-key-aaaaaaaa");
        Instant issuedAt = Instant.now().minusSeconds(120);
        String token = encode(key, claims(issuedAt, "mcp-tool-execution"));

        assertThrows(
                JwtException.class,
                () -> service(key).verify(token));
    }

    private DefaultMcpTenantExecutionTokenService service(SecretKey key) {
        JwtEncoder encoder = NimbusJwtEncoder.withSecretKey(key).build();
        JwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).build();
        return new DefaultMcpTenantExecutionTokenService(encoder, decoder);
    }

    private String encode(SecretKey key, JwtClaimsSet claims) {
        return NimbusJwtEncoder.withSecretKey(key).build()
                .encode(JwtEncoderParameters.from(claims))
                .getTokenValue();
    }

    private JwtClaimsSet claims(Instant issuedAt, String purpose) {
        return JwtClaimsSet.builder()
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(30))
                .subject("user-a")
                .claim(AuthenticationConstants.CLAIM_TENANT_ID, "tenant-a")
                .claim("username", "lawyer-a")
                .claim(AuthenticationConstants.CLAIM_ROLES, List.of("LAWYER"))
                .claim("purpose", purpose)
                .build();
    }

    private SecretKey secret(String value) {
        return new SecretKeySpec(
                value.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256");
    }

    private TenantContext tenantA() {
        return new TenantContext(
                "tenant-a",
                "user-a",
                "lawyer-a",
                Set.of(UserRole.LAWYER));
    }
}
