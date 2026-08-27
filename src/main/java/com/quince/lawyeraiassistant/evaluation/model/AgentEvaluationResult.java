package com.quince.lawyeraiassistant.evaluation.model;

import java.math.BigDecimal;
import java.util.List;

public record AgentEvaluationResult(
        String caseId,
        double totalScore,
        double minimumScore,
        boolean passed,
        double conceptCoverage,
        List<String> matchedConcepts,
        List<String> missingConcepts,
        List<String> matchedForbiddenClaims,
        List<EvaluationDimensionScore> dimensions,
        BigDecimal actualCostUsd,
        BigDecimal maximumCostUsd,
        boolean withinCostBudget) {

    public AgentEvaluationResult {

        if (caseId == null
                || caseId.isBlank()) {

            throw new IllegalArgumentException(
                    "caseId must not be blank");
        }

        if (Double.isNaN(totalScore)
                || totalScore < 0
                || totalScore > 100) {

            throw new IllegalArgumentException(
                    "totalScore must be between 0 and 100");
        }

        if (Double.isNaN(minimumScore)
                || minimumScore < 0
                || minimumScore > 100) {

            throw new IllegalArgumentException(
                    "minimumScore must be between 0 and 100");
        }

        if (Double.isNaN(conceptCoverage)
                || conceptCoverage < 0
                || conceptCoverage > 1) {

            throw new IllegalArgumentException(
                    "conceptCoverage must be between 0 and 1");
        }

        actualCostUsd = requireNonNegative(
                actualCostUsd,
                "actualCostUsd");

        maximumCostUsd = requireNonNegative(
                maximumCostUsd,
                "maximumCostUsd");

        caseId = caseId.trim();

        matchedConcepts = copy(matchedConcepts);

        missingConcepts = copy(missingConcepts);

        matchedForbiddenClaims = copy(matchedForbiddenClaims);

        dimensions = dimensions == null
                ? List.of()
                : List.copyOf(dimensions);
    }

    /**
     * 向后兼容现有 Metrics 测试。
     */
    public AgentEvaluationResult(
            String caseId,
            double totalScore,
            double minimumScore,
            boolean passed,
            double conceptCoverage,
            List<String> matchedConcepts,
            List<String> missingConcepts,
            List<String> matchedForbiddenClaims,
            List<EvaluationDimensionScore> dimensions) {

        this(
                caseId,
                totalScore,
                minimumScore,
                passed,
                conceptCoverage,
                matchedConcepts,
                missingConcepts,
                matchedForbiddenClaims,
                dimensions,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                true);
    }

    private static List<String> copy(
            List<String> values) {

        return values == null
                ? List.of()
                : List.copyOf(values);
    }

    private static BigDecimal requireNonNegative(
            BigDecimal value,
            String field) {

        if (value == null
                || value.signum() < 0) {

            throw new IllegalArgumentException(
                    field + " must not be null or negative");
        }

        return value;
    }
}