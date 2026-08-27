package com.quince.lawyeraiassistant.agent.runtime.cost;

import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentTokenUsage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class ModelCostCalculator {

    public AgentCallCost calculate(
            ModelPricing pricing,
            AgentTokenUsage usage) {

        Objects.requireNonNull(pricing, "pricing must not be null");
        Objects.requireNonNull(usage, "usage must not be null");

        if (!usage.available()) {
            throw new IllegalArgumentException(
                    "Token usage must be available");
        }

        /*
         * USD cost:
         *
         * tokens × (USD / 1,000,000 tokens)
         *
         * Convert USD to micro-USD:
         *
         * tokens × price-per-million
         */
        BigDecimal promptMicros = BigDecimal.valueOf(usage.promptTokens())
                .multiply(
                        pricing.promptPricePerMillionTokens());

        BigDecimal completionMicros = BigDecimal.valueOf(usage.completionTokens())
                .multiply(
                        pricing.completionPricePerMillionTokens());

        long costMicros = promptMicros
                .add(completionMicros)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();

        return new AgentCallCost(
                pricing.model(),
                pricing.currency(),
                usage.promptTokens(),
                usage.completionTokens(),
                usage.totalTokens(),
                costMicros);
    }
}