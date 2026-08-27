package com.quince.lawyeraiassistant.evaluation.judge;

import java.util.List;

public record ModelJudgeRequest(
        String caseId,
        String category,
        String question,
        List<String> expectedConcepts,
        List<String> forbiddenClaims,
        String answer) {

    public ModelJudgeRequest {

        caseId = requireText(
                caseId,
                "caseId");

        category = requireText(
                category,
                "category");

        question = requireText(
                question,
                "question");

        expectedConcepts = expectedConcepts == null
                ? List.of()
                : List.copyOf(expectedConcepts);

        forbiddenClaims = forbiddenClaims == null
                ? List.of()
                : List.copyOf(forbiddenClaims);

        answer = requireText(
                answer,
                "answer");
    }

    private static String requireText(
            String value,
            String field) {

        if (value == null
                || value.isBlank()) {

            throw new IllegalArgumentException(
                    field + " must not be blank");
        }

        return value.trim();
    }
}