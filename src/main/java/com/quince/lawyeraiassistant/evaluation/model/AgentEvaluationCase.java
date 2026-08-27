package com.quince.lawyeraiassistant.evaluation.model;

import java.math.BigDecimal;
import java.util.List;

public record AgentEvaluationCase(
        String caseId,
        String category,
        String question,
        List<String> requiredConcepts,
        List<String> forbiddenClaims,
        int minimumAnswerLength,
        double minimumConceptCoverage,
        double minimumScore,
        BigDecimal maximumCostUsd) {

    public AgentEvaluationCase {

        caseId = requireText(
                caseId,
                "caseId");

        category = requireText(
                category,
                "category");

        question = requireText(
                question,
                "question");

        requiredConcepts = requiredConcepts == null
                ? List.of()
                : requiredConcepts.stream()
                        .filter(value -> value != null
                                && !value.isBlank())
                        .map(String::trim)
                        .distinct()
                        .toList();

        forbiddenClaims = forbiddenClaims == null
                ? List.of()
                : forbiddenClaims.stream()
                        .filter(value -> value != null
                                && !value.isBlank())
                        .map(String::trim)
                        .distinct()
                        .toList();

        if (minimumAnswerLength < 0) {
            throw new IllegalArgumentException(
                    "minimumAnswerLength must not be negative");
        }

        validateRatio(
                minimumConceptCoverage,
                "minimumConceptCoverage");

        if (minimumScore < 0
                || minimumScore > 100) {

            throw new IllegalArgumentException(
                    "minimumScore must be between 0 and 100");
        }

        if (maximumCostUsd == null
                || maximumCostUsd.signum() < 0) {

            throw new IllegalArgumentException(
                    "maximumCostUsd must not be null or negative");
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

    private static void validateRatio(
            double value,
            String field) {

        if (Double.isNaN(value)
                || value < 0
                || value > 1) {

            throw new IllegalArgumentException(
                    field + " must be between 0 and 1");
        }
    }
}