package com.quince.lawyeraiassistant.evaluation.live;

import com.quince.lawyeraiassistant.evaluation.judge.ModelJudgeResult;

import java.math.BigDecimal;
import java.util.List;

public record LiveEvaluationCaseReport(
                String caseId,
                String category,
                String agentStatus,
                double score,
                double minimumScore,
                boolean deterministicPassed,
                boolean passed,
                double conceptCoverage,
                List<String> missingConcepts,
                List<String> matchedForbiddenClaims,
                boolean withinCostBudget,
                long durationMs,
                int llmCalls,
                long promptTokens,
                long completionTokens,
                long totalTokens,
                int usageMissingCalls,
                long pricedLlmCalls,
                long pricingMissingCalls,
                BigDecimal costUsd,
                String answerPreview,
                ModelJudgeResult modelJudge,
                String errorType) {

        public LiveEvaluationCaseReport {

                missingConcepts = missingConcepts == null
                                ? List.of()
                                : List.copyOf(missingConcepts);

                matchedForbiddenClaims = matchedForbiddenClaims == null
                                ? List.of()
                                : List.copyOf(
                                                matchedForbiddenClaims);

                costUsd = costUsd == null
                                ? BigDecimal.ZERO
                                : costUsd;

                answerPreview = answerPreview == null
                                ? ""
                                : answerPreview;

                modelJudge = modelJudge == null
                                ? ModelJudgeResult.disabled()
                                : modelJudge;

                errorType = errorType == null
                                ? ""
                                : errorType;
        }
}