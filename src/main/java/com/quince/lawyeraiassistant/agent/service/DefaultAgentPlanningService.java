package com.quince.lawyeraiassistant.agent.service;

import com.quince.lawyeraiassistant.agent.model.AgentPlan;
import com.quince.lawyeraiassistant.agent.parser.AgentPlanParser;
import com.quince.lawyeraiassistant.agent.prompt.model.PlanningPromptContext;
import com.quince.lawyeraiassistant.prompt.builder.PromptBuilder;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * AgentPlanningService 的默认实现。
 *
 * <p>
 * 使用 LLM 生成 Planning 文本，
 * 并通过 AgentPlanParser 解析为结构化 AgentPlan。
 * </p>
 */
@Service
public class DefaultAgentPlanningService
                implements AgentPlanningService {

        private final ChatClient chatClient;

        private final PromptBuilder promptBuilder;

        private final AgentPlanParser agentPlanParser;

        public DefaultAgentPlanningService(
                        @Qualifier("agentPlanningChatClient") ChatClient chatClient,
                        PromptBuilder promptBuilder,
                        AgentPlanParser agentPlanParser) {

                this.chatClient = Objects.requireNonNull(
                                chatClient,
                                "agentPlanningChatClient must not be null");

                this.promptBuilder = Objects.requireNonNull(
                                promptBuilder,
                                "PromptBuilder must not be null");

                this.agentPlanParser = Objects.requireNonNull(
                                agentPlanParser,
                                "AgentPlanParser must not be null");
        }

        @Override
        public AgentPlan plan(
                        PlanningPromptContext context) {

                Objects.requireNonNull(
                                context,
                                "PlanningPromptContext must not be null");

                Prompt prompt = promptBuilder.buildPlanning(
                                context);

                String content = chatClient
                                .prompt(prompt)
                                .call()
                                .content();

                if (content == null
                                || content.isBlank()) {

                        throw new IllegalStateException(
                                        "Planning result must not be blank");
                }

                return agentPlanParser.parse(
                                content);
        }
}