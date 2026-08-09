package com.quince.lawyeraiassistant.agent.service;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.prompt.builder.FinalAnswerPromptContextBuilder;
import com.quince.lawyeraiassistant.agent.prompt.model.FinalAnswerPromptContext;
import com.quince.lawyeraiassistant.prompt.builder.PromptBuilder;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * AgentFinalAnswerService 默认实现。
 *
 * <p>
 * 基于当前 AgentContext 构建 Final Answer Prompt，
 * 调用 LLM 生成最终面向用户的自然语言答案。
 * </p>
 */
@Service
public class DefaultAgentFinalAnswerService
        implements AgentFinalAnswerService {

    private final ChatClient chatClient;

    private final PromptBuilder promptBuilder;

    private final FinalAnswerPromptContextBuilder promptContextBuilder;

    public DefaultAgentFinalAnswerService(
            @Qualifier("agentFinalAnswerChatClient") ChatClient chatClient,
            PromptBuilder promptBuilder,
            FinalAnswerPromptContextBuilder promptContextBuilder) {

        this.chatClient = Objects.requireNonNull(
                chatClient,
                "agentFinalAnswerChatClient must not be null");

        this.promptBuilder = Objects.requireNonNull(
                promptBuilder,
                "PromptBuilder must not be null");

        this.promptContextBuilder = Objects.requireNonNull(
                promptContextBuilder,
                "FinalAnswerPromptContextBuilder must not be null");
    }

    @Override
    public String generate(
            AgentContext context) {

        Objects.requireNonNull(
                context,
                "AgentContext must not be null");

        FinalAnswerPromptContext promptContext = promptContextBuilder.build(
                context);

        Prompt prompt = promptBuilder.buildFinalAnswer(
                promptContext);

        String content = chatClient
                .prompt(prompt)
                .call()
                .content();

        if (content == null
                || content.isBlank()) {

            throw new IllegalStateException(
                    "Final answer must not be blank");
        }

        return content.trim();
    }
}