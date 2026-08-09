package com.quince.lawyeraiassistant.agent.service;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentTask;
import com.quince.lawyeraiassistant.agent.model.ReflectionDecisionResponse;
import com.quince.lawyeraiassistant.agent.model.ReflectionResult;
import com.quince.lawyeraiassistant.agent.prompt.builder.ReflectionPromptContextBuilder;
import com.quince.lawyeraiassistant.agent.prompt.model.ReflectionPromptContext;
import com.quince.lawyeraiassistant.prompt.builder.PromptBuilder;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * AgentReflectionService 默认实现。
 *
 * <p>
 * 使用 LLM 判断当前 AgentTask 的执行结果
 * 是否足够完成当前任务，并返回结构化 ReflectionResult。
 * </p>
 */
@Service
public class DefaultAgentReflectionService
        implements AgentReflectionService {

    private final ChatClient chatClient;

    private final PromptBuilder promptBuilder;

    private final ReflectionPromptContextBuilder promptContextBuilder;

    public DefaultAgentReflectionService(
            @Qualifier("agentReflectionChatClient") ChatClient chatClient,
            PromptBuilder promptBuilder,
            ReflectionPromptContextBuilder promptContextBuilder) {

        this.chatClient = Objects.requireNonNull(
                chatClient,
                "agentReflectionChatClient must not be null");

        this.promptBuilder = Objects.requireNonNull(
                promptBuilder,
                "PromptBuilder must not be null");

        this.promptContextBuilder = Objects.requireNonNull(
                promptContextBuilder,
                "ReflectionPromptContextBuilder must not be null");
    }

    @Override
    public ReflectionResult reflect(
            AgentContext context,
            AgentTask task) {

        Objects.requireNonNull(
                context,
                "AgentContext must not be null");

        Objects.requireNonNull(
                task,
                "AgentTask must not be null");

        ReflectionPromptContext promptContext = promptContextBuilder.build(
                context,
                task);

        Prompt prompt = promptBuilder.buildReflection(
                promptContext);

        ReflectionDecisionResponse response = chatClient
                .prompt(
                        prompt)
                .call()
                .entity(
                        ReflectionDecisionResponse.class);

        if (response == null) {
            throw new IllegalStateException(
                    "Reflection result must not be null");
        }

        if (response.decision() == null) {
            throw new IllegalStateException(
                    "Reflection decision must not be null");
        }

        if (response.summary() == null
                || response.summary()
                        .isBlank()) {

            throw new IllegalStateException(
                    "Reflection summary must not be blank");
        }

        return ReflectionResult.of(
                response.decision(),
                response.summary());
    }
}