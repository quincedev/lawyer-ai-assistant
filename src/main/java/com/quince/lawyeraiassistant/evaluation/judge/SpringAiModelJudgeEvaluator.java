package com.quince.lawyeraiassistant.evaluation.judge;

import com.quince.lawyeraiassistant.agent.runtime.cost.AgentCostProperties;
import com.quince.lawyeraiassistant.agent.runtime.cost.ModelCostCalculator;
import com.quince.lawyeraiassistant.agent.runtime.cost.ModelPricing;
import com.quince.lawyeraiassistant.agent.runtime.cost.ModelPricingCatalog;
import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentTokenUsage;
import com.quince.lawyeraiassistant.evaluation.judge.metrics.ModelJudgeMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@Component
@Profile("!mcp-server")
@ConditionalOnProperty(
        prefix = "app.evaluation.judge",
        name = "enabled",
        havingValue = "true")
public class SpringAiModelJudgeEvaluator implements ModelJudgeEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            SpringAiModelJudgeEvaluator.class);

    private final ChatModel chatModel;
    private final ModelJudgePromptFactory promptFactory;
    private final ModelJudgeResponseParser responseParser;
    private final ModelJudgeProperties properties;
    private final ModelPricingCatalog pricingCatalog;
    private final ModelCostCalculator costCalculator;
    private final AgentCostProperties costProperties;
    private final ModelJudgeMetrics judgeMetrics;

    public SpringAiModelJudgeEvaluator(
            ChatModel chatModel,
            ModelJudgePromptFactory promptFactory,
            ModelJudgeResponseParser responseParser,
            ModelJudgeProperties properties,
            ModelPricingCatalog pricingCatalog,
            ModelCostCalculator costCalculator,
            AgentCostProperties costProperties,
            ModelJudgeMetrics judgeMetrics) {
        this.chatModel = Objects.requireNonNull(chatModel, "chatModel must not be null");
        this.promptFactory = Objects.requireNonNull(promptFactory, "promptFactory must not be null");
        this.responseParser = Objects.requireNonNull(responseParser, "responseParser must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.pricingCatalog = Objects.requireNonNull(pricingCatalog, "pricingCatalog must not be null");
        this.costCalculator = Objects.requireNonNull(costCalculator, "costCalculator must not be null");
        this.costProperties = Objects.requireNonNull(costProperties, "costProperties must not be null");
        this.judgeMetrics = Objects.requireNonNull(judgeMetrics, "judgeMetrics must not be null");
    }

    @Override
    public ModelJudgeResult evaluate(ModelJudgeRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        Instant startedAt = Instant.now();

        try {
            Prompt prompt = promptFactory.create(request);

            /*
             * Direct ChatModel use keeps Judge usage outside the Agent
             * performance, token, and cost attribution boundary.
             */
            ChatResponse response = chatModel.call(prompt);
            validateResponse(response);

            ModelJudgeResponsePayload payload = responseParser.parse(
                    response.getResult().getOutput().getText());
            var providerUsage = response.getMetadata() == null
                    ? null
                    : response.getMetadata().getUsage();
            AgentTokenUsage tokenUsage = AgentTokenUsage.fromProviderValues(
                    providerUsage == null ? null : providerUsage.getPromptTokens(),
                    providerUsage == null ? null : providerUsage.getCompletionTokens(),
                    providerUsage == null ? null : providerUsage.getTotalTokens());
            String model = extractModel(response);
            JudgeCostAttribution costAttribution = calculateCost(model, tokenUsage);
            long durationMs = elapsedMillis(startedAt);
            boolean passed = payload.overallScore() >= properties.getMinimumScore()
                    && !payload.criticalRisk();

            ModelJudgeResult result = new ModelJudgeResult(
                    ModelJudgeStatus.COMPLETED,
                    passed,
                    payload.overallScore(),
                    payload.legalCorrectnessScore(),
                    payload.completenessScore(),
                    payload.relevanceScore(),
                    payload.riskControlScore(),
                    payload.reasons(),
                    payload.risks(),
                    model,
                    durationMs,
                    tokenUsage.promptTokens(),
                    tokenUsage.completionTokens(),
                    tokenUsage.totalTokens(),
                    tokenUsage.available(),
                    costAttribution.pricingAvailable(),
                    costAttribution.costUsd(),
                    "");

            judgeMetrics.record(result);
            LOGGER.info(
                    "Model judge evaluation completed. caseId={}, model={}, score={}, "
                            + "passed={}, criticalRisk={}, promptTokens={}, completionTokens={}, "
                            + "totalTokens={}, pricingAvailable={}, costUsd={}, durationMs={}",
                    request.caseId(), model, payload.overallScore(), passed,
                    payload.criticalRisk(), tokenUsage.promptTokens(),
                    tokenUsage.completionTokens(), tokenUsage.totalTokens(),
                    costAttribution.pricingAvailable(), costAttribution.costUsd(), durationMs);
            return result;
        } catch (RuntimeException exception) {
            long durationMs = elapsedMillis(startedAt);
            ModelJudgeResult result = ModelJudgeResult.failed(
                    exception.getClass().getSimpleName(),
                    properties.isFailOpen(),
                    durationMs);
            judgeMetrics.record(result);
            LOGGER.warn(
                    "Model judge evaluation failed. caseId={}, failOpen={}, durationMs={}, errorType={}",
                    request.caseId(), properties.isFailOpen(), durationMs,
                    exception.getClass().getSimpleName(), exception);
            return result;
        }
    }

    private JudgeCostAttribution calculateCost(
            String model,
            AgentTokenUsage tokenUsage) {
        if (!tokenUsage.available()) {
            return JudgeCostAttribution.unavailable();
        }
        Optional<ModelPricing> pricing = pricingCatalog.find(model);
        if (pricing.isEmpty()) {
            return JudgeCostAttribution.unavailable();
        }
        if (!costProperties.isEnabled()) {
            return JudgeCostAttribution.available(BigDecimal.ZERO);
        }
        return JudgeCostAttribution.available(
                costCalculator.calculate(pricing.get(), tokenUsage).cost());
    }

    private String extractModel(ChatResponse response) {
        if (response.getMetadata() == null
                || response.getMetadata().getModel() == null) {
            return "";
        }
        return String.valueOf(response.getMetadata().getModel());
    }

    private void validateResponse(ChatResponse response) {
        if (response == null
                || response.getResult() == null
                || response.getResult().getOutput() == null) {
            throw new IllegalStateException("Model judge returned no result");
        }
        String text = response.getResult().getOutput().getText();
        if (text == null || text.isBlank()) {
            throw new IllegalStateException("Model judge returned an empty response");
        }
    }

    private long elapsedMillis(Instant startedAt) {
        return Duration.between(startedAt, Instant.now()).toMillis();
    }

    private record JudgeCostAttribution(
            boolean pricingAvailable,
            BigDecimal costUsd) {
        private JudgeCostAttribution {
            costUsd = costUsd == null ? BigDecimal.ZERO : costUsd;
        }

        private static JudgeCostAttribution available(BigDecimal costUsd) {
            return new JudgeCostAttribution(true, costUsd);
        }

        private static JudgeCostAttribution unavailable() {
            return new JudgeCostAttribution(false, BigDecimal.ZERO);
        }
    }
}
