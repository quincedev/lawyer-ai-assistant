package com.quince.lawyeraiassistant.agent.service;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentTask;
import com.quince.lawyeraiassistant.agent.prompt.builder.RuntimeReasonPromptContextBuilder;
import com.quince.lawyeraiassistant.agent.prompt.model.RuntimeReasonPromptContext;
import com.quince.lawyeraiassistant.agent.service.support.BoundedLlmCallExecutor;
import com.quince.lawyeraiassistant.agent.service.support.RetryableLlmResponseException;
import com.quince.lawyeraiassistant.prompt.builder.PromptBuilder;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * AgentRuntimeReasonService 默认实现。
 *
 * <p>
 * Runtime Reason 属于 LLM 阶段。对于 LLM 调用异常、null 或空白响应，
 * 最多执行一次补偿重试。
 * </p>
 */
@Service
public class DefaultAgentRuntimeReasonService
        implements AgentRuntimeReasonService {

    private static final String STAGE =
            "RUNTIME_REASON";

    private final ChatClient chatClient;

    private final PromptBuilder promptBuilder;

    private final RuntimeReasonPromptContextBuilder promptContextBuilder;

    private final BoundedLlmCallExecutor llmCallExecutor;

    public DefaultAgentRuntimeReasonService(
            @Qualifier("agentRuntimeReasonChatClient") ChatClient chatClient,
            PromptBuilder promptBuilder,
            RuntimeReasonPromptContextBuilder promptContextBuilder,
            BoundedLlmCallExecutor llmCallExecutor) {

        this.chatClient = Objects.requireNonNull(
                chatClient,
                "agentRuntimeReasonChatClient must not be null");

        this.promptBuilder = Objects.requireNonNull(
                promptBuilder,
                "PromptBuilder must not be null");

        this.promptContextBuilder = Objects.requireNonNull(
                promptContextBuilder,
                "RuntimeReasonPromptContextBuilder must not be null");

        this.llmCallExecutor = Objects.requireNonNull(
                llmCallExecutor,
                "BoundedLlmCallExecutor must not be null");
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

        String content = llmCallExecutor.execute(
                STAGE,
                () -> requestRuntimeReason(
                        prompt));

        return content.trim();
    }

    private String requestRuntimeReason(
            Prompt prompt) {

        final String content;

        try {

            content = chatClient
                    .prompt(prompt)
                    .call()
                    .content();

        } catch (RuntimeException exception) {

            throw new RetryableLlmResponseException(
                    "Runtime reason LLM call failed",
                    exception);
        }

        if (content == null
                || content.isBlank()) {

            throw new RetryableLlmResponseException(
                    "Runtime reason result must not be blank");
        }

        return content;
    }
}
