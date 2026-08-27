package com.quince.lawyeraiassistant.evaluation.judge;

import java.util.List;

public record ModelJudgeResponsePayload(
        double overallScore,
        double legalCorrectnessScore,
        double completenessScore,
        double relevanceScore,
        double riskControlScore,
        boolean criticalRisk,
        List<String> reasons,
        List<String> risks) {

    public ModelJudgeResponsePayload {

        validateScore(
                overallScore,
                "overallScore");

        validateScore(
                legalCorrectnessScore,
                "legalCorrectnessScore");

        validateScore(
                completenessScore,
                "completenessScore");

        validateScore(
                relevanceScore,
                "relevanceScore");

        validateScore(
                riskControlScore,
                "riskControlScore");

        reasons = sanitize(reasons);

        risks = sanitize(risks);
    }

    private static List<String> sanitize(
            List<String> values) {

        if (values == null) {
            return List.of();
        }

        return values.stream()
                .filter(value -> value != null
                        && !value.isBlank())
                .map(String::trim)
                .limit(20)
                .toList();
    }

    private static void validateScore(
            double score,
            String field) {

        if (Double.isNaN(score)
                || Double.isInfinite(score)
                || score < 0.0D
                || score > 100.0D) {

            throw new IllegalArgumentException(
                    field
                            + " must be between 0 and 100");
        }
    }
}