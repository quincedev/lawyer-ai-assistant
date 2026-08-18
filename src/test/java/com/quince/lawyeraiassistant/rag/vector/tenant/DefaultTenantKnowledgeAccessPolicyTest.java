package com.quince.lawyeraiassistant.rag.vector.tenant;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.ai.document.Document;

import com.quince.lawyeraiassistant.security.SecurityTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SecurityTest
class DefaultTenantKnowledgeAccessPolicyTest {

    private DefaultTenantKnowledgeAccessPolicy policy;

    @BeforeEach
    void setUp() {

        policy = new DefaultTenantKnowledgeAccessPolicy();
    }

    @Test
    void shouldAllowSharedKnowledgeForTenant() {

        Document document = document(
                KnowledgeScope.SHARED,
                null);

        assertTrue(
                policy.canAccess(
                        document,
                        "tenant-a"));
    }

    @Test
    void shouldAllowTenantOwnedKnowledge() {

        Document document = document(
                KnowledgeScope.TENANT,
                "tenant-a");

        assertTrue(
                policy.canAccess(
                        document,
                        "tenant-a"));
    }

    @Test
    void shouldRejectAnotherTenantsKnowledge() {

        Document document = document(
                KnowledgeScope.TENANT,
                "tenant-b");

        assertFalse(
                policy.canAccess(
                        document,
                        "tenant-a"));
    }

    @Test
    void shouldRejectTenantKnowledgeWithoutTenantId() {

        Document document = document(
                KnowledgeScope.TENANT,
                null);

        assertFalse(
                policy.canAccess(
                        document,
                        "tenant-a"));
    }

    @Test
    void shouldFailClosedWhenScopeIsMissing() {

        Document document = new Document(
                "doc-1",
                "unknown document",
                Map.of());

        assertFalse(
                policy.canAccess(
                        document,
                        "tenant-a"));

        assertFalse(
                policy.canAccessSharedOnly(
                        document));
    }

    @Test
    void shouldAllowOnlySharedKnowledgeForSharedOnlyAccess() {

        Document shared = document(
                KnowledgeScope.SHARED,
                null);

        Document tenant = document(
                KnowledgeScope.TENANT,
                "tenant-a");

        assertTrue(
                policy.canAccessSharedOnly(
                        shared));

        assertFalse(
                policy.canAccessSharedOnly(
                        tenant));
    }

    private Document document(
            KnowledgeScope scope,
            String tenantId) {

        Map<String, Object> metadata = new java.util.LinkedHashMap<>();

        metadata.put(
                KnowledgeMetadata.KNOWLEDGE_SCOPE,
                scope.name());

        if (tenantId != null) {

            metadata.put(
                    KnowledgeMetadata.TENANT_ID,
                    tenantId);
        }

        return new Document(
                "doc-" + scope.name(),
                "test document",
                metadata);
    }
}