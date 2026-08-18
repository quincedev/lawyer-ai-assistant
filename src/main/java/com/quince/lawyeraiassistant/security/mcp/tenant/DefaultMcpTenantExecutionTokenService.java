package com.quince.lawyeraiassistant.security.mcp.tenant;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.quince.lawyeraiassistant.security.identity.AuthenticationConstants;
import com.quince.lawyeraiassistant.security.identity.UserRole;
import com.quince.lawyeraiassistant.security.tenant.TenantContext;

@Service
public final class DefaultMcpTenantExecutionTokenService
        implements McpTenantExecutionTokenService {

    private static final String CLAIM_PURPOSE = "purpose";

    private static final String PURPOSE = "mcp-tool-execution";

    private static final String CLAIM_USERNAME = "username";

    private static final Duration TOKEN_TTL = Duration.ofMinutes(
            2);

    private final JwtEncoder jwtEncoder;

    private final JwtDecoder jwtDecoder;

    public DefaultMcpTenantExecutionTokenService(
            JwtEncoder jwtEncoder,
            JwtDecoder jwtDecoder) {

        this.jwtEncoder = Objects.requireNonNull(
                jwtEncoder,
                "jwtEncoder must not be null");

        this.jwtDecoder = Objects.requireNonNull(
                jwtDecoder,
                "jwtDecoder must not be null");
    }

    @Override
    public String issue(
            TenantContext tenantContext) {

        Objects.requireNonNull(
                tenantContext,
                "tenantContext must not be null");

        Instant now = Instant.now();

        List<String> roles = tenantContext.roles()
                .stream()
                .map(
                        Enum::name)
                .sorted()
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuedAt(
                        now)
                .expiresAt(
                        now.plus(
                                TOKEN_TTL))
                .subject(
                        tenantContext.userId())
                .claim(
                        AuthenticationConstants.CLAIM_TENANT_ID,
                        tenantContext.tenantId())
                .claim(
                        CLAIM_USERNAME,
                        tenantContext.username())
                .claim(
                        AuthenticationConstants.CLAIM_ROLES,
                        roles)
                .claim(
                        CLAIM_PURPOSE,
                        PURPOSE)
                .build();

        return jwtEncoder
                .encode(
                        JwtEncoderParameters.from(
                                claims))
                .getTokenValue();
    }

    @Override
    public TenantContext verify(
            String token) {

        Objects.requireNonNull(
                token,
                "token must not be null");

        if (token.isBlank()) {

            throw new IllegalArgumentException(
                    "token must not be blank");
        }

        Jwt jwt = jwtDecoder.decode(
                token.trim());

        String purpose = jwt.getClaimAsString(
                CLAIM_PURPOSE);

        if (!PURPOSE.equals(
                purpose)) {

            throw new IllegalArgumentException(
                    "Invalid MCP tenant execution token purpose");
        }

        String tenantId = jwt.getClaimAsString(
                AuthenticationConstants.CLAIM_TENANT_ID);

        String username = jwt.getClaimAsString(
                CLAIM_USERNAME);

        List<String> roleClaims = jwt.getClaimAsStringList(
                AuthenticationConstants.CLAIM_ROLES);

        if (roleClaims == null
                || roleClaims.isEmpty()) {

            throw new IllegalArgumentException(
                    "MCP tenant execution token roles are missing");
        }

        Set<UserRole> roles = roleClaims.stream()
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