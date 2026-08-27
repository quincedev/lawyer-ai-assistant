package com.quince.lawyeraiassistant.advisor;

import com.quince.lawyeraiassistant.agent.runtime.cost.AgentCostProperties;
import com.quince.lawyeraiassistant.agent.runtime.cost.ModelCostCalculator;
import com.quince.lawyeraiassistant.agent.runtime.cost.ModelPricingCatalog;
import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceContext;
import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceMetrics;
import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentTokenUsage;

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

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class LoggingAdvisorV2 implements CallAdvisor {

        private static final Logger log = LoggerFactory.getLogger(
                        LoggingAdvisorV2.class);

        /**
         * 作为最外层 Advisor：
         * 统计完整 Advisor 链、RAG 检索和模型调用的总耗时。
         */
        private static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 100;

        private static final String UNKNOWN_CONVERSATION_ID = "unknown";

        private static final String UNKNOWN_MODEL = "unknown";

        /**
         * 防止调试日志一次输出过多内容。
         */
        private static final int MAX_LOG_CONTENT_LENGTH = 4000;

        private final AgentPerformanceContext performanceContext;

        private final ModelPricingCatalog pricingCatalog;

        private final ModelCostCalculator costCalculator;

        private final AgentCostProperties costProperties;

        public LoggingAdvisorV2(
                        AgentPerformanceContext performanceContext,
                        ModelPricingCatalog pricingCatalog,
                        ModelCostCalculator costCalculator,
                        AgentCostProperties costProperties) {

                this.performanceContext = Objects.requireNonNull(
                                performanceContext,
                                "performanceContext must not be null");

                this.pricingCatalog = Objects.requireNonNull(
                                pricingCatalog,
                                "pricingCatalog must not be null");

                this.costCalculator = Objects.requireNonNull(
                                costCalculator,
                                "costCalculator must not be null");

                this.costProperties = Objects.requireNonNull(
                                costProperties,
                                "costProperties must not be null");
        }

        @Override
        public ChatClientResponse adviseCall(
                        ChatClientRequest request,
                        CallAdvisorChain chain) {

                String requestId = UUID.randomUUID().toString();

                String conversationId = extractConversationId(request);

                long startTime = System.nanoTime();

                RequestStatus status = RequestStatus.FAILED;

                AgentTokenUsage tokenUsage = AgentTokenUsage.unavailable();

                String model = UNKNOWN_MODEL;

                log.info(
                                "AI request started, requestId={}, "
                                                + "conversationId={}, advisor={}",
                                requestId,
                                conversationId,
                                getName());

                logRequestDebugInfo(
                                requestId,
                                request,
                                chain);

                try {

                        ChatClientResponse response = chain.nextCall(request);

                        status = RequestStatus.SUCCESS;

                        Usage providerUsage = extractUsage(response);

                        tokenUsage = AgentTokenUsage.fromProviderValues(
                                        getPromptTokens(providerUsage),
                                        getCompletionTokens(providerUsage),
                                        getTotalTokens(providerUsage));

                        model = extractModel(response);

                        log.info(
                                        "AI request succeeded, requestId={}, "
                                                        + "conversationId={}, "
                                                        + "model={}, "
                                                        + "promptTokens={}, "
                                                        + "completionTokens={}, "
                                                        + "totalTokens={}, "
                                                        + "usageAvailable={}",
                                        requestId,
                                        conversationId,
                                        model,
                                        tokenUsage.promptTokens(),
                                        tokenUsage.completionTokens(),
                                        tokenUsage.totalTokens(),
                                        tokenUsage.available());

                        logResponseDebugInfo(
                                        requestId,
                                        response);

                        return response;

                } catch (RuntimeException exception) {

                        log.error(
                                        "AI request failed, requestId={}, "
                                                        + "conversationId={}, "
                                                        + "exceptionType={}, message={}",
                                        requestId,
                                        conversationId,
                                        exception.getClass()
                                                        .getSimpleName(),
                                        exception.getMessage(),
                                        exception);

                        throw exception;

                } finally {

                        long durationNanos = System.nanoTime() - startTime;

                        long durationMillis = TimeUnit.NANOSECONDS.toMillis(
                                        durationNanos);

                        Optional<AgentPerformanceMetrics> currentMetrics = performanceContext.current();

                        if (currentMetrics.isPresent()) {

                                AgentPerformanceMetrics metrics = currentMetrics.get();

                                metrics.recordLlmCall(
                                                durationMillis);

                                metrics.recordTokenUsage(
                                                tokenUsage);

                                recordCost(
                                                metrics,
                                                model,
                                                tokenUsage);
                        }

                        log.info(
                                        "AI request finished, requestId={}, "
                                                        + "conversationId={}, "
                                                        + "model={}, "
                                                        + "status={}, durationMs={}",
                                        requestId,
                                        conversationId,
                                        model,
                                        status,
                                        durationMillis);
                }
        }

        private void recordCost(
                        AgentPerformanceMetrics metrics,
                        String model,
                        AgentTokenUsage tokenUsage) {

                if (!costProperties.isEnabled()) {
                        return;
                }

                /*
                 * Token Usage 不完整时，22B 已经通过
                 * usageMissingCalls 记录。
                 *
                 * 此处不把它重复统计为 pricing missing，
                 * 因为当前根本无法进行成本计算。
                 */
                if (tokenUsage == null
                                || !tokenUsage.available()) {

                        return;
                }

                pricingCatalog.find(model)
                                .ifPresentOrElse(
                                                pricing -> {

                                                        var cost = costCalculator.calculate(
                                                                        pricing,
                                                                        tokenUsage);

                                                        metrics.recordLlmCost(cost);

                                                        log.debug(
                                                                        "LLM cost calculated, "
                                                                                        + "model={}, currency={}, "
                                                                                        + "costMicros={}, "
                                                                                        + "promptTokens={}, "
                                                                                        + "completionTokens={}",
                                                                        cost.model(),
                                                                        cost.currency(),
                                                                        cost.costMicros(),
                                                                        cost.promptTokens(),
                                                                        cost.completionTokens());
                                                },
                                                () -> {

                                                        metrics.recordPricingMissing();

                                                        log.warn(
                                                                        "LLM pricing unavailable, "
                                                                                        + "model={}, "
                                                                                        + "promptTokens={}, "
                                                                                        + "completionTokens={}",
                                                                        model,
                                                                        tokenUsage.promptTokens(),
                                                                        tokenUsage.completionTokens());
                                                });
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
                                || response.chatResponse()
                                                .getResult() == null
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
                                || response.chatResponse()
                                                .getMetadata() == null) {

                        return null;
                }

                return response.chatResponse()
                                .getMetadata()
                                .getUsage();
        }

        private String extractModel(
                        ChatClientResponse response) {

                if (response == null
                                || response.chatResponse() == null
                                || response.chatResponse()
                                                .getMetadata() == null) {

                        return UNKNOWN_MODEL;
                }

                String model = response.chatResponse()
                                .getMetadata()
                                .getModel();

                if (model == null || model.isBlank()) {
                        return UNKNOWN_MODEL;
                }

                return model.trim();
        }

        private Integer getPromptTokens(
                        Usage usage) {

                return usage == null
                                ? null
                                : usage.getPromptTokens();
        }

        private Integer getCompletionTokens(
                        Usage usage) {

                return usage == null
                                ? null
                                : usage.getCompletionTokens();
        }

        private Integer getTotalTokens(
                        Usage usage) {

                return usage == null
                                ? null
                                : usage.getTotalTokens();
        }

        private String truncate(
                        String content) {

                if (content == null) {
                        return null;
                }

                if (content.length() <= MAX_LOG_CONTENT_LENGTH) {

                        return content;
                }

                return content.substring(
                                0,
                                MAX_LOG_CONTENT_LENGTH)
                                + "...[truncated]";
        }

        private Map<String, Object> sanitizeContext(
                        Map<String, Object> context) {

                /*
                 * 当前保持既有行为。
                 * 后续安全增强可进一步过滤敏感字段。
                 */
                return context;
        }

        @Override
        public String getName() {

                return LoggingAdvisorV2.class
                                .getSimpleName();
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