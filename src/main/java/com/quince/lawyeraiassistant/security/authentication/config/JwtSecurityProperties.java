package com.quince.lawyeraiassistant.security.authentication.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "security.jwt")
public class JwtSecurityProperties {

    @NotBlank
    private String issuer = "lawyer-ai-assistant";

    @NotBlank
    private String secret;

    @NotNull
    private Duration accessTokenTtl = Duration.ofHours(1);

    public String getIssuer() {

        return issuer;
    }

    public void setIssuer(
            String issuer) {

        this.issuer = issuer;
    }

    public String getSecret() {

        return secret;
    }

    public void setSecret(
            String secret) {

        this.secret = secret;
    }

    public Duration getAccessTokenTtl() {

        return accessTokenTtl;
    }

    public void setAccessTokenTtl(
            Duration accessTokenTtl) {

        this.accessTokenTtl = accessTokenTtl;
    }

    @AssertTrue(message = "security.jwt.secret must contain at least 32 UTF-8 bytes")
    public boolean isSecretLengthValid() {

        if (secret == null || secret.isBlank()) {
            /*
             * @NotBlank 负责报告空值。
             */
            return true;
        }

        return secret.getBytes(
                StandardCharsets.UTF_8).length >= 32;
    }

    @AssertTrue(message = "security.jwt.access-token-ttl must be greater than zero")
    public boolean isAccessTokenTtlValid() {

        if (accessTokenTtl == null) {
            return true;
        }

        return !accessTokenTtl.isZero()
                && !accessTokenTtl.isNegative();
    }
}