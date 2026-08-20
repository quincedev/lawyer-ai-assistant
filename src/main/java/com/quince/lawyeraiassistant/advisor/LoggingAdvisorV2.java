package com.quince.lawyeraiassistant.advisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceContext;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class LoggingAdvisorV2 implements CallAdvisor {

        private static final Logger log = LoggerFactory.getLogger(LoggingAdvisorV2.class);

        /**
         * 作为最外层 Advisor：
         * 统计完整 Advisor 链、RAG 检索和模型调用的总耗时。
         */
        private static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 100;

        private static final String UNKNOWN_CONVERSATION_ID = "unknown";

        /**
         * 防止调试日志一次输出过多内容。
         */
        private static final int MAX_LOG_CONTENT_LENGTH = 4000;

        private final AgentPerformanceContext performanceContext;

        public LoggingAdvisorV2(
                        AgentPerformanceContext performanceContext) {

                this.performanceContext = java.util.Objects.requireNonNull(
                                performanceContext,
                                "AgentPerformanceContext must not be null");
        }

        @Override
        public ChatClientResponse adviseCall(
                        ChatClientRequest request,
                        CallAdvisorChain chain) {

                String requestId = UUID.randomUUID().toString();
                String conversationId = extractConversationId(request);

                long startTime = System.nanoTime();
                RequestStatus status = RequestStatus.FAILED;

                log.info(
                                "AI request started, requestId={}, conversationId={}, advisor={}",
                                requestId,
                                conversationId,
                                getName());

                logRequestDebugInfo(requestId, request, chain);

                try {
                        ChatClientResponse response = chain.nextCall(request);

                        status = RequestStatus.SUCCESS;

                        Usage usage = extractUsage(response);

                        log.info(
                                        "AI request succeeded, requestId={}, conversationId={}, "
                                                        + "promptTokens={}, completionTokens={}, totalTokens={}",
                                        requestId,
                                        conversationId,
                                        getPromptTokens(usage),
                                        getCompletionTokens(usage),
                                        getTotalTokens(usage));

                        logResponseDebugInfo(requestId, response);

                        return response;
                } catch (RuntimeException exception) {
                        log.error(
                                        "AI request failed, requestId={}, conversationId={}, "
                                                        + "exceptionType={}, message={}",
                                        requestId,
                                        conversationId,
                                        exception.getClass().getSimpleName(),
                                        exception.getMessage(),
                                        exception);

                        throw exception;
                } finally {

                        long durationNanos = System.nanoTime() - startTime;

                        long durationMillis = TimeUnit.NANOSECONDS.toMillis(
                                        durationNanos);

                        performanceContext
                                        .current()
                                        .ifPresent(
                                                        metrics -> metrics.recordLlmCall(
                                                                        durationMillis));

                        log.info(
                                        "AI request finished, requestId={}, conversationId={}, "
                                                        + "status={}, durationMs={}",
                                        requestId,
                                        conversationId,
                                        status,
                                        durationMillis);
                }
        }

        private void logRequestDebugInfo(
                        String requestId,
                        ChatClientRequest request,
                        CallAdvisorChain chain) {

                if (!log.isDebugEnabled()) {
                        return;
                }

                log.debug(
                                "AI request prompt started, requestId={}",
                                requestId);

                if (request.prompt() == null) {
                        log.debug(
                                        "AI request prompt is null, requestId={}",
                                        requestId);
                } else {
                        for (Message message : request.prompt().getInstructions()) {

                                log.debug(
                                                "AI prompt message, requestId={}, "
                                                                + "messageType={}, content={}",
                                                requestId,
                                                message.getMessageType(),
                                                truncate(message.getText()));
                        }
                }

                log.debug(
                                "AI advisor context, requestId={}, context={}",
                                requestId,
                                sanitizeContext(request.context()));

                log.debug(
                                "AI advisor chain, requestId={}, advisors={}",
                                requestId,
                                chain.getCallAdvisors()
                                                .stream()
                                                .map(advisor -> advisor.getName()
                                                                + "("
                                                                + advisor.getOrder()
                                                                + ")")
                                                .toList());

                log.debug(
                                "AI request prompt finished, requestId={}",
                                requestId);
        }

        private void logResponseDebugInfo(
                        String requestId,
                        ChatClientResponse response) {

                if (!log.isDebugEnabled()) {
                        return;
                }

                if (response == null
                                || response.chatResponse() == null
                                || response.chatResponse().getResult() == null
                                || response.chatResponse()
                                                .getResult()
                                                .getOutput() == null) {

                        log.debug(
                                        "AI response content unavailable, requestId={}",
                                        requestId);

                        return;
                }

                String content = response.chatResponse()
                                .getResult()
                                .getOutput()
                                .getText();

                log.debug(
                                "AI response content, requestId={}, content={}",
                                requestId,
                                truncate(content));

                log.debug(
                                "AI response context, requestId={}, context={}",
                                requestId,
                                sanitizeContext(response.context()));
        }

        private String extractConversationId(
                        ChatClientRequest request) {

                Object value = request.context()
                                .get(ChatMemory.CONVERSATION_ID);

                return value == null
                                ? UNKNOWN_CONVERSATION_ID
                                : value.toString();
        }

        private Usage extractUsage(
                        ChatClientResponse response) {

                if (response == null
                                || response.chatResponse() == null
                                || response.chatResponse().getMetadata() == null) {

                        return null;
                }

                return response.chatResponse()
                                .getMetadata()
                                .getUsage();
        }

        private Integer getPromptTokens(Usage usage) {
                return usage == null
                                ? null
                                : usage.getPromptTokens();
        }

        private Integer getCompletionTokens(Usage usage) {
                return usage == null
                                ? null
                                : usage.getCompletionTokens();
        }

        private Integer getTotalTokens(Usage usage) {
                return usage == null
                                ? null
                                : usage.getTotalTokens();
        }

        private String truncate(String content) {

                if (content == null) {
                        return null;
                }

                if (content.length() <= MAX_LOG_CONTENT_LENGTH) {
                        return content;
                }

                return content.substring(0, MAX_LOG_CONTENT_LENGTH)
                                + "...[truncated]";
        }

        private Map<String, Object> sanitizeContext(
                        Map<String, Object> context) {

                /*
                 * 当前学习阶段先原样返回。
                 * 生产环境应过滤 API Key、Token、身份证号、
                 * 用户上传文档正文等敏感内容。
                 */
                return context;
        }

        @Override
        public String getName() {
                return LoggingAdvisorV2.class.getSimpleName();
        }

        @Override
        public int getOrder() {
                return ORDER;
        }

        private enum RequestStatus {
                SUCCESS,
                FAILED
        }
}