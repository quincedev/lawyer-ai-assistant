package com.quince.lawyeraiassistant.rag.vector.tenant;

import java.util.Map;
import java.util.Objects;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

@Component
public final class DefaultTenantKnowledgeAccessPolicy
        implements TenantKnowledgeAccessPolicy {

    @Override
    public boolean canAccess(
            Document document,
            String tenantId) {

        Objects.requireNonNull(
                document,
                "document must not be null");

        Objects.requireNonNull(
                tenantId,
                "tenantId must not be null");

        Map<String, Object> metadata = document.getMetadata();

        String scope = metadataValue(
                metadata,
                KnowledgeMetadata.KNOWLEDGE_SCOPE);

        if (KnowledgeScope.SHARED
                .name()
                .equals(
                        scope)) {

            return true;
        }

        if (!KnowledgeScope.TENANT
                .name()
                .equals(
                        scope)) {

            return false;
        }

        String documentTenantId = metadataValue(
                metadata,
                KnowledgeMetadata.TENANT_ID);

        return tenantId.equals(
                documentTenantId);
    }

    @Override
    public boolean canAccessSharedOnly(
            Document document) {

        Objects.requireNonNull(
                document,
                "document must not be null");

        return KnowledgeScope.SHARED
                .name()
                .equals(
                        metadataValue(
                                document.getMetadata(),
                                KnowledgeMetadata.KNOWLEDGE_SCOPE));
    }

    private String metadataValue(
            Map<String, Object> metadata,
            String key) {

        Object value = metadata.get(
                key);

        if (value == null) {
            return null;
        }

        String normalized = value.toString()
                .trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}