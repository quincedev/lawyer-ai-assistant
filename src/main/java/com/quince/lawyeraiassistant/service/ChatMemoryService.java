package com.quince.lawyeraiassistant.service;

import java.util.Objects;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.quince.lawyeraiassistant.dto.request.ChatMemoryRequest;
import com.quince.lawyeraiassistant.dto.response.ChatMemoryResponse;
import com.quince.lawyeraiassistant.security.tenant.TenantContext;
import com.quince.lawyeraiassistant.security.tenant.TenantContextProvider;
import com.quince.lawyeraiassistant.security.tenant.conversation.TenantConversationKeyFactory;

@Service
public class ChatMemoryService {

        private final ChatClient memoryChatClient;

        private final TenantContextProvider tenantContextProvider;

        private final TenantConversationKeyFactory conversationKeyFactory;

        public ChatMemoryService(
                        @Qualifier("memoryChatClient") ChatClient memoryChatClient,
                        TenantContextProvider tenantContextProvider,
                        TenantConversationKeyFactory conversationKeyFactory) {

                this.memoryChatClient = Objects.requireNonNull(
                                memoryChatClient,
                                "memoryChatClient must not be null");

                this.tenantContextProvider = Objects.requireNonNull(
                                tenantContextProvider,
                                "tenantContextProvider must not be null");

                this.conversationKeyFactory = Objects.requireNonNull(
                                conversationKeyFactory,
                                "conversationKeyFactory must not be null");
        }

        public ChatMemoryResponse chat(
                        ChatMemoryRequest request) {

                Objects.requireNonNull(
                                request,
                                "request must not be null");

                String conversationId = request.getConversationId()
                                .strip();

                String message = request.getMessage()
                                .strip();

                TenantContext tenantContext = tenantContextProvider.current();

                String internalConversationKey = conversationKeyFactory.create(
                                tenantContext,
                                conversationId);

                String answer = memoryChatClient
                                .prompt()
                                .user(
                                                message)
                                .advisors(
                                                advisorSpec -> advisorSpec.param(
                                                                ChatMemory.CONVERSATION_ID,
                                                                internalConversationKey))
                                .call()
                                .content();

                /*
                 * Important:
                 *
                 * Only expose the client-visible conversationId.
                 * Never expose the internal tenant-scoped memory key.
                 */
                return new ChatMemoryResponse(
                                conversationId,
                                answer);
        }
}