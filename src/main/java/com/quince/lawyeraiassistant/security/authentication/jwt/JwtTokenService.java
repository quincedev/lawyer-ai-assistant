package com.quince.lawyeraiassistant.security.authentication.jwt;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.quince.lawyeraiassistant.security.authentication.config.JwtSecurityProperties;
import com.quince.lawyeraiassistant.security.identity.ApplicationUser;
import com.quince.lawyeraiassistant.security.identity.AuthenticationConstants;

@Service
public final class JwtTokenService {

    private final JwtEncoder jwtEncoder;

    private final JwtSecurityProperties properties;

    public JwtTokenService(
            JwtEncoder jwtEncoder,
            JwtSecurityProperties properties) {

        this.jwtEncoder = Objects.requireNonNull(
                jwtEncoder,
                "jwtEncoder must not be null");

        this.properties = Objects.requireNonNull(
                properties,
                "properties must not be null");
    }

    public String createAccessToken(
            ApplicationUser user) {

        Objects.requireNonNull(
                user,
                "user must not be null");

        Instant now = Instant.now();

        Instant expiresAt = now.plus(
                properties.getAccessTokenTtl());

        List<String> roles = user.roles()
                .stream()
                .map(Enum::name)
                .sorted()
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(
                        properties.getIssuer())
                .issuedAt(
                        now)
                .expiresAt(
                        expiresAt)
                .subject(
                        user.id())
                .claim(
                        "username",
                        user.username())
                .claim(
                        AuthenticationConstants.CLAIM_TENANT_ID,
                        user.tenantId())
                .claim(
                        AuthenticationConstants.CLAIM_ROLES,
                        roles)
                .build();

        return jwtEncoder
                .encode(
                        JwtEncoderParameters.from(
                                claims))
                .getTokenValue();
    }
}