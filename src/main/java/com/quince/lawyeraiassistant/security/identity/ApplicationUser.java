package com.quince.lawyeraiassistant.security.identity;

import java.util.Objects;
import java.util.Set;

/**
 * Minimal application user identity model.
 *
 * Persistence is intentionally separated from this model.
 */
public record ApplicationUser(
        String id,
        String tenantId,
        String username,
        String passwordHash,
        Set<UserRole> roles,
        boolean enabled) {

    public ApplicationUser {

        id = normalizeRequired(
                id,
                "id");

        tenantId = normalizeRequired(
                tenantId,
                "tenantId");

        username = normalizeRequired(
                username,
                "username");

        passwordHash = normalizeRequired(
                passwordHash,
                "passwordHash");

        Objects.requireNonNull(
                roles,
                "roles must not be null");

        if (roles.isEmpty()) {

            throw new IllegalArgumentException(
                    "roles must not be empty");
        }

        roles = Set.copyOf(
                roles);
    }

    private static String normalizeRequired(
            String value,
            String fieldName) {

        Objects.requireNonNull(
                value,
                fieldName + " must not be null");

        String normalized = value.trim();

        if (normalized.isBlank()) {

            throw new IllegalArgumentException(
                    fieldName + " must not be blank");
        }

        return normalized;
    }
}