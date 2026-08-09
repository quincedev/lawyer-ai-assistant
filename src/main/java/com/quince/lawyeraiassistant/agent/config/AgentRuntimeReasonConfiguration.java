package com.quince.lawyeraiassistant.agent.config;

import com.quince.lawyeraiassistant.advisor.LoggingAdvisorV2;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Agent Runtime Reason ChatClient 配置。
 */
@Configuration
public class AgentRuntimeReasonConfiguration {

    @Bean("agentRuntimeReasonChatClient")
    public ChatClient agentRuntimeReasonChatClient(
            ChatClient.Builder builder,
            LoggingAdvisorV2 loggingAdvisorV2) {

        return builder
                .defaultAdvisors(
                        loggingAdvisorV2)
                .build();
    }
}