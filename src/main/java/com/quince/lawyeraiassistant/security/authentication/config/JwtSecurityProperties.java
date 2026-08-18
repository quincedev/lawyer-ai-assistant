package com.quince.lawyeraiassistant.security.authentication.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

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
}