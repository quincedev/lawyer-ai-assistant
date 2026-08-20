package com.quince.lawyeraiassistant.agent.service;

import java.util.Objects;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentTask;
import com.quince.lawyeraiassistant.agent.model.ReflectionDecisionResponse;
import com.quince.lawyeraiassistant.agent.model.ReflectionResult;
import com.quince.lawyeraiassistant.agent.prompt.builder.ReflectionPromptContextBuilder;
import com.quince.lawyeraiassistant.agent.prompt.model.ReflectionPromptContext;
import com.quince.lawyeraiassistant.agent.service.support.BoundedLlmCallExecutor;
import com.quince.lawyeraiassistant.agent.service.support.RetryableLlmResponseException;
import com.quince.lawyeraiassistant.prompt.builder.PromptBuilder;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * AgentReflectionService 默认实现。
 *
 * <p>
 * 使用 LLM 判断当前 AgentTask 的执行结果
 * 是否足够完成当前任务，并返回结构化 ReflectionResult。
 * </p>
 *
 * <p>
 * 对空响应、Structured Output 转换失败、
 * 必要字段为空等情况执行一次 bounded retry。
 * </p>
 */
@Service
public class DefaultAgentReflectionService
                implements AgentReflectionService {

        private static final String STAGE = "REFLECTION";

        private final ChatClient chatClient;

        private final PromptBuilder promptBuilder;

        private final ReflectionPromptContextBuilder promptContextBuilder;

        private final BoundedLlmCallExecutor llmCallExecutor;

        public DefaultAgentReflectionService(
                        @Qualifier("agentReflectionChatClient") ChatClient chatClient,
                        PromptBuilder promptBuilder,
                        ReflectionPromptContextBuilder promptContextBuilder,
                        BoundedLlmCallExecutor llmCallExecutor) {

                this.chatClient = Objects.requireNonNull(
                                chatClient,
                                "agentReflectionChatClient must not be null");

                this.promptBuilder = Objects.requireNonNull(
                                promptBuilder,
                                "PromptBuilder must not be null");

                this.promptContextBuilder = Objects.requireNonNull(
                                promptContextBuilder,
                                "ReflectionPromptContextBuilder must not be null");

                this.llmCallExecutor = Objects.requireNonNull(
                                llmCallExecutor,
                                "BoundedLlmCallExecutor must not be null");
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

                ReflectionDecisionResponse response = llmCallExecutor.execute(
                                STAGE,
                                () -> requestReflection(
                                                prompt));

                return ReflectionResult.of(
                                response.decision(),
                                response.summary());
        }

        private ReflectionDecisionResponse requestReflection(
                        Prompt prompt) {

                final ReflectionDecisionResponse response;

                try {

                        response = chatClient
                                        .prompt(
                                                        prompt)
                                        .call()
                                        .entity(
                                                        ReflectionDecisionResponse.class);

                } catch (RuntimeException exception) {

                        /*
                         * 包含：
                         *
                         * BeanOutputConverter conversion failure
                         * Jackson MismatchedInputException
                         * transient ChatClient call failure
                         *
                         * 统一视为本次 structured LLM response
                         * 无法消费，允许补偿一次。
                         */
                        throw new RetryableLlmResponseException(
                                        "Reflection structured response conversion failed",
                                        exception);
                }

                validateResponse(
                                response);

                return response;
        }

        private void validateResponse(
                        ReflectionDecisionResponse response) {

                if (response == null) {

                        throw new RetryableLlmResponseException(
                                        "Reflection result must not be null");
                }

                if (response.decision() == null) {

                        throw new RetryableLlmResponseException(
                                        "Reflection decision must not be null");
                }

                if (response.summary() == null
                                || response.summary()
                                                .isBlank()) {

                        throw new RetryableLlmResponseException(
                                        "Reflection summary must not be blank");
                }
        }
}