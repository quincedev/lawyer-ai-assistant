package com.quince.lawyeraiassistant.agent.service;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentTask;
import com.quince.lawyeraiassistant.agent.prompt.builder.RuntimeReasonPromptContextBuilder;
import com.quince.lawyeraiassistant.agent.prompt.model.RuntimeReasonPromptContext;
import com.quince.lawyeraiassistant.prompt.builder.PromptBuilder;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * AgentRuntimeReasonService 默认实现。
 */
@Service
public class DefaultAgentRuntimeReasonService
        implements AgentRuntimeReasonService {

    private final ChatClient chatClient;

    private final PromptBuilder promptBuilder;

    private final RuntimeReasonPromptContextBuilder promptContextBuilder;

    public DefaultAgentRuntimeReasonService(
            @Qualifier("agentRuntimeReasonChatClient") ChatClient chatClient,
            PromptBuilder promptBuilder,
            RuntimeReasonPromptContextBuilder promptContextBuilder) {

        this.chatClient = Objects.requireNonNull(
                chatClient,
                "agentRuntimeReasonChatClient must not be null");

        this.promptBuilder = Objects.requireNonNull(
                promptBuilder,
                "PromptBuilder must not be null");

        this.promptContextBuilder = Objects.requireNonNull(
                promptContextBuilder,
                "RuntimeReasonPromptContextBuilder must not be null");
    }

    @Override
    public String reason(
            AgentContext context,
            AgentTask task) {

        Objects.requireNonNull(
                context,
                "AgentContext must not be null");

        Objects.requireNonNull(
                task,
                "AgentTask must not be null");

        RuntimeReasonPromptContext promptContext = promptContextBuilder.build(
                context,
                task);

        Prompt prompt = promptBuilder.buildRuntimeReason(
                promptContext);

        String content = chatClient
                .prompt(
                        prompt)
                .call()
                .content();

        if (content == null
                || content.isBlank()) {

            throw new IllegalStateException(
                    "Runtime reason result must not be blank");
        }

        return content.trim();
    }
}