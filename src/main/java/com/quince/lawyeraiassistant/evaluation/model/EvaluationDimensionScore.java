package com.quince.lawyeraiassistant.evaluation.model;

public record EvaluationDimensionScore(
        String dimension,
        double score,
        double maximumScore,
        String explanation) {

    public EvaluationDimensionScore {

        if (dimension == null
                || dimension.isBlank()) {

            throw new IllegalArgumentException(
                    "dimension must not be blank");
        }

        if (score < 0
                || maximumScore < 0
                || score > maximumScore) {

            throw new IllegalArgumentException(
                    "Invalid dimension score");
        }

        explanation = explanation == null
                ? ""
                : explanation.trim();
    }
}