package com.quince.lawyeraiassistant.security.tenant.conversation;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.messages.UserMessage;

import com.quince.lawyeraiassistant.security.SecurityTest;
import com.quince.lawyeraiassistant.security.identity.UserRole;
import com.quince.lawyeraiassistant.security.tenant.TenantContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SecurityTest
class TenantChatMemoryRepositoryIsolationTest {

    private ChatMemoryRepository repository;

    private TenantConversationKeyFactory keyFactory;

    @BeforeEach
    void setUp() {

        repository = new InMemoryChatMemoryRepository();

        keyFactory = new DefaultTenantConversationKeyFactory();
    }

    @Test
    void shouldIsolateSameConversationIdAcrossTenants() {

        TenantContext tenantA = tenant(
                "tenant-a",
                "user-001");

        TenantContext tenantB = tenant(
                "tenant-b",
                "user-003");

        String tenantAKey = keyFactory.create(
                tenantA,
                "conversation-001");

        String tenantBKey = keyFactory.create(
                tenantB,
                "conversation-001");

        repository.saveAll(
                tenantAKey,
                java.util.List.of(
                        new UserMessage(
                                "tenant-a-secret")));

        assertEquals(
                1,
                repository.findByConversationId(
                        tenantAKey)
                        .size());

        assertTrue(
                repository.findByConversationId(
                        tenantBKey)
                        .isEmpty());
    }

    @Test
    void shouldIsolateSameConversationIdAcrossUsers() {

        TenantContext firstUser = tenant(
                "tenant-a",
                "user-001");

        TenantContext secondUser = tenant(
                "tenant-a",
                "user-002");

        String firstKey = keyFactory.create(
                firstUser,
                "conversation-001");

        String secondKey = keyFactory.create(
                secondUser,
                "conversation-001");

        repository.saveAll(
                firstKey,
                java.util.List.of(
                        new UserMessage(
                                "private-memory")));

        assertEquals(
                1,
                repository.findByConversationId(
                        firstKey)
                        .size());

        assertTrue(
                repository.findByConversationId(
                        secondKey)
                        .isEmpty());
    }

    private TenantContext tenant(
            String tenantId,
            String userId) {

        return new TenantContext(
                tenantId,
                userId,
                userId,
                Set.of(
                        UserRole.LAWYER));
    }
}