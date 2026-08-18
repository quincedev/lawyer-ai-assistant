package com.quince.lawyeraiassistant.rag.vector.tenant;

import java.util.Objects;

import org.springframework.stereotype.Component;

@Component
public final class DefaultTenantKnowledgeFilterFactory
        implements TenantKnowledgeFilterFactory {

    @Override
    public String createForTenant(
            String tenantId) {

        Objects.requireNonNull(
                tenantId,
                "tenantId must not be null");

        String normalized = tenantId.trim();

        if (normalized.isEmpty()) {

            throw new IllegalArgumentException(
                    "tenantId must not be blank");
        }

        return KnowledgeMetadata.KNOWLEDGE_SCOPE
                + " == '"
                + escape(
                        KnowledgeScope.SHARED.name())
                + "' || ("
                + KnowledgeMetadata.KNOWLEDGE_SCOPE
                + " == '"
                + escape(
                        KnowledgeScope.TENANT.name())
                + "' && "
                + KnowledgeMetadata.TENANT_ID
                + " == '"
                + escape(
                        normalized)
                + "')";
    }

    @Override
    public String createSharedOnly() {

        return KnowledgeMetadata.KNOWLEDGE_SCOPE
                + " == '"
                + KnowledgeScope.SHARED.name()
                + "'";
    }

    private String escape(
            String value) {

        return value.replace(
                "'",
                "''");
    }
}