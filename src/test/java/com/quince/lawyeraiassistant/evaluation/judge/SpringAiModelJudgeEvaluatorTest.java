package com.quince.lawyeraiassistant.evaluation.judge;

import com.quince.lawyeraiassistant.agent.runtime.cost.AgentCostProperties;
import com.quince.lawyeraiassistant.agent.runtime.cost.ModelCostCalculator;
import com.quince.lawyeraiassistant.agent.runtime.cost.ModelPricing;
import com.quince.lawyeraiassistant.agent.runtime.cost.ModelPricingCatalog;
import com.quince.lawyeraiassistant.evaluation.judge.metrics.ModelJudgeMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpringAiModelJudgeEvaluatorTest {

    private ChatModel chatModel;
    private ModelJudgePromptFactory promptFactory;
    private ModelJudgeResponseParser responseParser;
    private ModelJudgeProperties properties;
    private ModelPricingCatalog pricingCatalog;
    private ModelJudgeMetrics metrics;
    private SpringAiModelJudgeEvaluator evaluator;

    @BeforeEach
    void setUp() {
        chatModel = mock(ChatModel.class);
        promptFactory = mock(ModelJudgePromptFactory.class);
        responseParser = mock(ModelJudgeResponseParser.class);
        properties = new ModelJudgeProperties();
        pricingCatalog = mock(ModelPricingCatalog.class);
        metrics = mock(ModelJudgeMetrics.class);

        when(promptFactory.create(any(ModelJudgeRequest.class)))
                .thenReturn(new Prompt("Judge test prompt"));

        evaluator = new SpringAiModelJudgeEvaluator(
                chatModel,
                promptFactory,
                responseParser,
                properties,
                pricingCatalog,
                new ModelCostCalculator(),
                new AgentCostProperties(),
                metrics);
    }

    @Test
    void shouldReturnCompletedResultWithIsolatedCost() {
        stubSuccessfulPayload(92.0D, false);
        when(pricingCatalog.find("deepseek-v4-flash"))
                .thenReturn(Optional.of(new ModelPricing(
                        "deepseek-v4-flash",
                        "USD",
                        new BigDecimal("0.10"),
                        new BigDecimal("0.20"))));

        ModelJudgeResult result = evaluator.evaluate(request());

        assertEquals(ModelJudgeStatus.COMPLETED, result.status());
        assertTrue(result.passed());
        assertEquals(92.0D, result.overallScore(), 0.000001D);
        assertEquals(1_000, result.promptTokens());
        assertEquals(500, result.completionTokens());
        assertEquals(1_500, result.totalTokens());
        assertTrue(result.usageAvailable());
        assertTrue(result.pricingAvailable());
        assertEquals(0, new BigDecimal("0.0002").compareTo(result.costUsd()));

        ArgumentCaptor<ModelJudgeResult> captor =
                ArgumentCaptor.forClass(ModelJudgeResult.class);
        verify(metrics).record(captor.capture());
        assertEquals(ModelJudgeStatus.COMPLETED, captor.getValue().status());
    }

    @Test
    void shouldFailJudgeWhenScoreIsBelowMinimum() {
        properties.setMinimumScore(75.0D);
        stubSuccessfulPayload(70.0D, false);
        when(pricingCatalog.find("deepseek-v4-flash"))
                .thenReturn(Optional.empty());

        ModelJudgeResult result = evaluator.evaluate(request());

        assertEquals(ModelJudgeStatus.COMPLETED, result.status());
        assertFalse(result.passed());
        assertEquals(70.0D, result.overallScore(), 0.000001D);
    }

    @Test
    void shouldFailJudgeWhenCriticalRiskExists() {
        stubSuccessfulPayload(90.0D, true);
        when(pricingCatalog.find("deepseek-v4-flash"))
                .thenReturn(Optional.empty());

        assertFalse(evaluator.evaluate(request()).passed());
    }

    @Test
    void shouldFailOpenWhenJudgeParsingFails() {
        properties.setFailOpen(true);
        stubParsingFailure();

        ModelJudgeResult result = evaluator.evaluate(request());

        assertEquals(ModelJudgeStatus.FAILED, result.status());
        assertTrue(result.passed());
        assertEquals("IllegalStateException", result.errorType());
        verify(metrics).record(result);
    }

    @Test
    void shouldFailClosedWhenJudgeParsingFails() {
        properties.setFailOpen(false);
        stubParsingFailure();

        ModelJudgeResult result = evaluator.evaluate(request());

        assertEquals(ModelJudgeStatus.FAILED, result.status());
        assertFalse(result.passed());
        verify(metrics).record(result);
    }

    private void stubSuccessfulPayload(double score, boolean criticalRisk) {
        ChatResponse response = successfulResponse();
        when(chatModel.call(any(Prompt.class))).thenReturn(response);
        when(responseParser.parse(any(String.class)))
                .thenReturn(new ModelJudgeResponsePayload(
                        score, score, score, score, score,
                        criticalRisk,
                        List.of("法律结论正确"),
                        criticalRisk ? List.of("存在严重误导性法律结论") : List.of()));
    }

    private void stubParsingFailure() {
        ChatResponse response = successfulResponse();
        when(chatModel.call(any(Prompt.class))).thenReturn(response);
        when(responseParser.parse(any(String.class)))
                .thenThrow(new IllegalStateException("Invalid judge JSON"));
    }

    private ChatResponse successfulResponse() {
        ChatResponse response = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
        when(response.getResult().getOutput().getText())
                .thenReturn("{\"overallScore\":92}");
        when(response.getMetadata().getModel())
                .thenReturn("deepseek-v4-flash");
        when(response.getMetadata().getUsage().getPromptTokens()).thenReturn(1_000);
        when(response.getMetadata().getUsage().getCompletionTokens()).thenReturn(500);
        when(response.getMetadata().getUsage().getTotalTokens()).thenReturn(1_500);
        return response;
    }

    private ModelJudgeRequest request() {
        return new ModelJudgeRequest(
                "labor-case-001",
                "labor-law",
                "用人单位违法解除劳动合同怎么办？",
                List.of("赔偿金", "经济补偿标准的二倍"),
                List.of("违法解除不需要赔偿"),
                "违法解除应支付赔偿金。");
    }
}
