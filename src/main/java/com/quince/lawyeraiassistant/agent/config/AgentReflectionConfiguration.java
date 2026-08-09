package com.quince.lawyeraiassistant.agent.config;

import com.quince.lawyeraiassistant.advisor.LoggingAdvisorV2;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Agent Reflection ChatClient 配置。
 */
@Configuration
public class AgentReflectionConfiguration {

    @Bean("agentReflectionChatClient")
    public ChatClient agentReflectionChatClient(
            ChatClient.Builder builder,
            LoggingAdvisorV2 loggingAdvisorV2) {

        return builder
                .defaultAdvisors(
                        loggingAdvisorV2)
                .build();
    }
}