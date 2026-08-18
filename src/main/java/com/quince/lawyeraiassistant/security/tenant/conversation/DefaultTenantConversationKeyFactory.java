package com.quince.lawyeraiassistant.security.tenant.conversation;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.quince.lawyeraiassistant.security.tenant.TenantContext;

/**
 * Creates an internal conversation key scoped by:
 *
 * tenant + user + external conversationId.
 *
 * <p>
 * The external conversationId must never be used directly as
 * the ChatMemory key in a multi-tenant environment.
 * </p>
 */
@Component
public final class DefaultTenantConversationKeyFactory
        implements TenantConversationKeyFactory {

    @Override
    public String create(
            TenantContext tenantContext,
            String conversationId) {

        Objects.requireNonNull(
                tenantContext,
                "tenantContext must not be null");

        String normalizedConversationId = requireText(
                conversationId,
                "conversationId");

        return encode(
                tenantContext.tenantId())
                + encode(
                        tenantContext.userId())
                + encode(
                        normalizedConversationId);
    }

    private String encode(
            String value) {

        return value.length()
                + ":"
                + value;
    }

    private String requireText(
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