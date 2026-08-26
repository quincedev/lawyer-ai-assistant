package com.quince.lawyeraiassistant.rag.config;

import com.quince.lawyeraiassistant.advisor.LoggingAdvisorV2;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RAG ChatClient 配置。
 *
 * <p>
 * 检索已经由 RetrievalOrchestrator 显式完成，
 * 因此该 ChatClient 不再挂载 RetrievalAugmentationAdvisor。
 * </p>
 */
@Configuration
@ConditionalOnRagEnabled
public class RagChatConfiguration {

    @Bean("ragChatClient")
    public ChatClient ragChatClient(
            ChatClient.Builder builder,
            LoggingAdvisorV2 loggingAdvisorV2) {

        return builder
                .defaultAdvisors(loggingAdvisorV2)
                .build();
    }
}