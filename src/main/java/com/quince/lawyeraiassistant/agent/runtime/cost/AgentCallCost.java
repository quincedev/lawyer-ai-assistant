package com.quince.lawyeraiassistant.agent.runtime.cost;

import java.math.BigDecimal;

public record AgentCallCost(
        String model,
        String currency,
        long promptTokens,
        long completionTokens,
        long totalTokens,
        long costMicros) {

    private static final BigDecimal MICROS_PER_UNIT = BigDecimal.valueOf(1_000_000L);

    public AgentCallCost {

        if (promptTokens < 0
                || completionTokens < 0
                || totalTokens < 0
                || costMicros < 0) {

            throw new IllegalArgumentException(
                    "Token and cost values must not be negative");
        }
    }

    public BigDecimal cost() {

        return BigDecimal.valueOf(costMicros)
                .divide(MICROS_PER_UNIT);
    }

    public double costAsDouble() {

        return cost().doubleValue();
    }
}