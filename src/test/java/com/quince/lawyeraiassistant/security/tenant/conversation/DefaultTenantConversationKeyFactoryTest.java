package com.quince.lawyeraiassistant.security.tenant.conversation;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.quince.lawyeraiassistant.security.identity.UserRole;
import com.quince.lawyeraiassistant.security.tenant.TenantContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultTenantConversationKeyFactoryTest {

    private DefaultTenantConversationKeyFactory factory;

    @BeforeEach
    void setUp() {

        factory = new DefaultTenantConversationKeyFactory();
    }

    @Test
    void shouldCreateSameKeyForSameTenantUserAndConversation() {

        TenantContext tenantContext = tenant(
                "tenant-a",
                "user-001");

        String first = factory.create(
                tenantContext,
                "conversation-001");

        String second = factory.create(
                tenantContext,
                "conversation-001");

        assertEquals(
                first,
                second);
    }

    @Test
    void shouldCreateDifferentKeysForDifferentTenants() {

        String first = factory.create(
                tenant(
                        "tenant-a",
                        "user-001"),
                "conversation-001");

        String second = factory.create(
                tenant(
                        "tenant-b",
                        "user-001"),
                "conversation-001");

        assertNotEquals(
                first,
                second);
    }

    @Test
    void shouldCreateDifferentKeysForDifferentUsersInSameTenant() {

        String first = factory.create(
                tenant(
                        "tenant-a",
                        "user-001"),
                "conversation-001");

        String second = factory.create(
                tenant(
                        "tenant-a",
                        "user-002"),
                "conversation-001");

        assertNotEquals(
                first,
                second);
    }

    @Test
    void shouldCreateDifferentKeysForDifferentConversations() {

        TenantContext tenantContext = tenant(
                "tenant-a",
                "user-001");

        String first = factory.create(
                tenantContext,
                "conversation-001");

        String second = factory.create(
                tenantContext,
                "conversation-002");

        assertNotEquals(
                first,
                second);
    }

    @Test
    void shouldNormalizeConversationId() {

        TenantContext tenantContext = tenant(
                "tenant-a",
                "user-001");

        String first = factory.create(
                tenantContext,
                " conversation-001 ");

        String second = factory.create(
                tenantContext,
                "conversation-001");

        assertEquals(
                first,
                second);
    }

    @Test
    void shouldRejectBlankConversationId() {

        assertThrows(
                IllegalArgumentException.class,
                () -> factory.create(
                        tenant(
                                "tenant-a",
                                "user-001"),
                        " "));
    }

    private TenantContext tenant(
            String tenantId,
            String userId) {

        return new TenantContext(
                tenantId,
                userId,
                "quince",
                Set.of(
                        UserRole.LAWYER));
    }
}