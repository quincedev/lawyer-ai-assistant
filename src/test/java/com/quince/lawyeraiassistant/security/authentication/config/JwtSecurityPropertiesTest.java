package com.quince.lawyeraiassistant.security.authentication.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class JwtSecurityPropertiesTest {

    @Test
    void shouldAcceptSecretWithAtLeast32Utf8Bytes() {

        JwtSecurityProperties properties = new JwtSecurityProperties();

        properties.setSecret(
                "12345678901234567890123456789012");

        assertTrue(
                properties.isSecretLengthValid());
    }

    @Test
    void shouldRejectShortSecret() {

        JwtSecurityProperties properties = new JwtSecurityProperties();

        properties.setSecret(
                "short-secret");

        assertFalse(
                properties.isSecretLengthValid());
    }

    @Test
    void shouldAcceptPositiveAccessTokenTtl() {

        JwtSecurityProperties properties = new JwtSecurityProperties();

        properties.setAccessTokenTtl(
                Duration.ofHours(1));

        assertTrue(
                properties.isAccessTokenTtlValid());
    }

    @Test
    void shouldRejectZeroOrNegativeAccessTokenTtl() {

        JwtSecurityProperties properties = new JwtSecurityProperties();

        properties.setAccessTokenTtl(
                Duration.ZERO);

        assertFalse(
                properties.isAccessTokenTtlValid());

        properties.setAccessTokenTtl(
                Duration.ofMinutes(-1));

        assertFalse(
                properties.isAccessTokenTtlValid());
    }
}