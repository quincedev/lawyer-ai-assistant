package com.quince.lawyeraiassistant.security.identity;

import java.util.Objects;
import java.util.Set;

/**
 * Represents the authenticated identity of the current request.
 *
 * <p>
 * This object contains trusted identity attributes derived from
 * the authentication layer, not from user-controlled request payloads.
 * </p>
 */
public record AuthenticatedIdentity(
        String userId,
        String username,
        String tenantId,
        Set<UserRole> roles) {

    public AuthenticatedIdentity {

        userId = normalizeRequired(
                userId,
                "userId");

        username = normalizeRequired(
                username,
                "username");

        tenantId = normalizeRequired(
                tenantId,
                "tenantId");

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

    public boolean hasRole(
            UserRole role) {

        Objects.requireNonNull(
                role,
                "role must not be null");

        return roles.contains(
                role);
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