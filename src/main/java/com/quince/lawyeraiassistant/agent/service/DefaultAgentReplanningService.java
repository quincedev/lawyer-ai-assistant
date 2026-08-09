package com.quince.lawyeraiassistant.agent.service;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentPlan;
import com.quince.lawyeraiassistant.agent.model.ReflectionResult;
import com.quince.lawyeraiassistant.agent.parser.AgentPlanParser;
import com.quince.lawyeraiassistant.agent.prompt.builder.ReplanningPromptContextBuilder;
import com.quince.lawyeraiassistant.agent.prompt.model.ReplanningPromptContext;
import com.quince.lawyeraiassistant.prompt.builder.PromptBuilder;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * AgentReplanningService 的默认实现。
 *
 * <p>
 * 当 Reflection 返回 REPLAN 时，
 * 根据当前 AgentContext、已有 Observation
 * 以及 Reflection 原因重新生成后续执行计划。
 * </p>
 */
@Service
public class DefaultAgentReplanningService
        implements AgentReplanningService {

    private final ChatClient chatClient;

    private final PromptBuilder promptBuilder;

    private final ReplanningPromptContextBuilder promptContextBuilder;

    private final AgentPlanParser agentPlanParser;

    public DefaultAgentReplanningService(
            @Qualifier("agentReplanningChatClient") ChatClient chatClient,
            PromptBuilder promptBuilder,
            ReplanningPromptContextBuilder promptContextBuilder,
            AgentPlanParser agentPlanParser) {

        this.chatClient = Objects.requireNonNull(
                chatClient,
                "agentReplanningChatClient must not be null");

        this.promptBuilder = Objects.requireNonNull(
                promptBuilder,
                "PromptBuilder must not be null");

        this.promptContextBuilder = Objects.requireNonNull(
                promptContextBuilder,
                "ReplanningPromptContextBuilder must not be null");

        this.agentPlanParser = Objects.requireNonNull(
                agentPlanParser,
                "AgentPlanParser must not be null");
    }

    @Override
    public AgentPlan replan(
            AgentContext context,
            ReflectionResult reflectionResult) {

        Objects.requireNonNull(
                context,
                "AgentContext must not be null");

        Objects.requireNonNull(
                reflectionResult,
                "ReflectionResult must not be null");

        if (!reflectionResult.shouldReplan()) {
            throw new IllegalArgumentException(
                    "Replanning requires REPLAN reflection decision");
        }

        ReplanningPromptContext promptContext = promptContextBuilder.build(
                context,
                reflectionResult);

        Prompt prompt = promptBuilder.buildReplanning(
                promptContext);

        String content = chatClient
                .prompt(prompt)
                .call()
                .content();

        if (content == null
                || content.isBlank()) {

            throw new IllegalStateException(
                    "Replanning result must not be blank");
        }

        return agentPlanParser.parse(
                content);
    }
}