package com.quince.lawyeraiassistant.rag.vector.tenant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.quince.lawyeraiassistant.security.SecurityTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SecurityTest
class DefaultTenantKnowledgeFilterFactoryTest {

    private DefaultTenantKnowledgeFilterFactory factory;

    @BeforeEach
    void setUp() {

        factory = new DefaultTenantKnowledgeFilterFactory();
    }

    @Test
    void shouldCreateTenantScopedFilter() {

        String filter = factory.createForTenant(
                "tenant-a");

        assertTrue(
                filter.contains(
                        "knowledge_scope == 'SHARED'"));

        assertTrue(
                filter.contains(
                        "knowledge_scope == 'TENANT'"));

        assertTrue(
                filter.contains(
                        "tenant_id == 'tenant-a'"));
    }

    @Test
    void shouldNotContainAnotherTenant() {

        String filter = factory.createForTenant(
                "tenant-a");

        assertFalse(
                filter.contains(
                        "tenant-b"));
    }

    @Test
    void shouldCreateSharedOnlyFilter() {

        String filter = factory.createSharedOnly();

        assertTrue(
                filter.contains(
                        "knowledge_scope == 'SHARED'"));

        assertFalse(
                filter.contains(
                        "tenant_id"));
    }

    @Test
    void shouldRejectBlankTenantId() {

        assertThrows(
                IllegalArgumentException.class,
                () -> factory.createForTenant(
                        " "));
    }
}