package com.quince.lawyeraiassistant.agent.service;

import java.util.Objects;

import com.quince.lawyeraiassistant.agent.model.AgentPlan;
import com.quince.lawyeraiassistant.agent.parser.AgentPlanParser;
import com.quince.lawyeraiassistant.agent.prompt.model.PlanningPromptContext;
import com.quince.lawyeraiassistant.agent.service.support.BoundedLlmCallExecutor;
import com.quince.lawyeraiassistant.agent.service.support.RetryableLlmResponseException;
import com.quince.lawyeraiassistant.prompt.builder.PromptBuilder;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * AgentPlanningService 的默认实现。
 *
 * <p>
 * 使用 LLM 生成 Planning 文本，
 * 并通过 AgentPlanParser 解析为结构化 AgentPlan。
 * </p>
 *
 * <p>
 * 对 LLM blank / null response 做一次 bounded retry。
 * </p>
 */
@Service
public class DefaultAgentPlanningService
                implements AgentPlanningService {

        private static final String STAGE = "PLANNING";

        private final ChatClient chatClient;

        private final PromptBuilder promptBuilder;

        private final AgentPlanParser agentPlanParser;

        private final BoundedLlmCallExecutor llmCallExecutor;

        public DefaultAgentPlanningService(
                        @Qualifier("agentPlanningChatClient") ChatClient chatClient,
                        PromptBuilder promptBuilder,
                        AgentPlanParser agentPlanParser,
                        BoundedLlmCallExecutor llmCallExecutor) {

                this.chatClient = Objects.requireNonNull(
                                chatClient,
                                "agentPlanningChatClient must not be null");

                this.promptBuilder = Objects.requireNonNull(
                                promptBuilder,
                                "PromptBuilder must not be null");

                this.agentPlanParser = Objects.requireNonNull(
                                agentPlanParser,
                                "AgentPlanParser must not be null");

                this.llmCallExecutor = Objects.requireNonNull(
                                llmCallExecutor,
                                "BoundedLlmCallExecutor must not be null");
        }

        @Override
        public AgentPlan plan(
                        PlanningPromptContext context) {

                Objects.requireNonNull(
                                context,
                                "PlanningPromptContext must not be null");

                Prompt prompt = promptBuilder.buildPlanning(
                                context);

                String content = llmCallExecutor.execute(
                                STAGE,
                                () -> requestPlanningContent(
                                                prompt));

                return agentPlanParser.parse(
                                content);
        }

        private String requestPlanningContent(
                        Prompt prompt) {

                final String content;

                try {

                        content = chatClient
                                        .prompt(
                                                        prompt)
                                        .call()
                                        .content();

                } catch (RuntimeException exception) {

                        throw new RetryableLlmResponseException(
                                        "Planning LLM call failed",
                                        exception);
                }

                if (content == null
                                || content.isBlank()) {

                        throw new RetryableLlmResponseException(
                                        "Planning result must not be blank");
                }

                return content;
        }
}