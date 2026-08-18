package com.quince.lawyeraiassistant.security.tenant;

import java.util.Objects;
import java.util.Set;

import com.quince.lawyeraiassistant.security.identity.UserRole;

public record TenantContext(
                String tenantId,
                String userId,
                String username,
                Set<UserRole> roles) {

        public TenantContext {

                tenantId = requireText(
                                tenantId,
                                "tenantId");

                userId = requireText(
                                userId,
                                "userId");

                username = requireText(
                                username,
                                "username");

                roles = Set.copyOf(
                                Objects.requireNonNull(
                                                roles,
                                                "roles must not be null"));

                if (roles.isEmpty()) {

                        throw new IllegalArgumentException(
                                        "roles must not be empty");
                }
        }

        public boolean hasRole(
                        UserRole role) {

                Objects.requireNonNull(
                                role,
                                "role must not be null");

                return roles.contains(
                                role);
        }

        private static String requireText(
                        String value,
                        String name) {

                Objects.requireNonNull(
                                value,
                                name + " must not be null");

                String normalized = value.trim();

                if (normalized.isBlank()) {

                        throw new IllegalArgumentException(
                                        name + " must not be blank");
                }

                return normalized;
        }
}