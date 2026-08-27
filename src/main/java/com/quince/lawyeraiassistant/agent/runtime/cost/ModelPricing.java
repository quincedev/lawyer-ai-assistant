package com.quince.lawyeraiassistant.agent.runtime.cost;

import java.math.BigDecimal;
import java.util.Objects;

public record ModelPricing(
        String model,
        String currency,
        BigDecimal promptPricePerMillionTokens,
        BigDecimal completionPricePerMillionTokens) {

    public ModelPricing {

        model = requireText(model, "model");
        currency = requireText(currency, "currency").toUpperCase();

        Objects.requireNonNull(
                promptPricePerMillionTokens,
                "promptPricePerMillionTokens must not be null");

        Objects.requireNonNull(
                completionPricePerMillionTokens,
                "completionPricePerMillionTokens must not be null");

        if (promptPricePerMillionTokens.signum() < 0) {
            throw new IllegalArgumentException(
                    "promptPricePerMillionTokens must not be negative");
        }

        if (completionPricePerMillionTokens.signum() < 0) {
            throw new IllegalArgumentException(
                    "completionPricePerMillionTokens must not be negative");
        }
    }

    private static String requireText(
            String value,
            String field) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " must not be blank");
        }

        return value.trim();
    }
}