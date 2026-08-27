package com.quince.lawyeraiassistant.evaluation.judge;

import java.math.BigDecimal;
import java.util.List;

public record ModelJudgeResult(
                ModelJudgeStatus status,
                boolean passed,
                double overallScore,
                double legalCorrectnessScore,
                double completenessScore,
                double relevanceScore,
                double riskControlScore,
                List<String> reasons,
                List<String> risks,
                String model,
                long durationMs,
                long promptTokens,
                long completionTokens,
                long totalTokens,
                boolean usageAvailable,
                boolean pricingAvailable,
                BigDecimal costUsd,
                String errorType) {

        public ModelJudgeResult {

                if (status == null) {
                        throw new IllegalArgumentException(
                                        "status must not be null");
                }

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

                reasons = reasons == null
                                ? List.of()
                                : List.copyOf(reasons);

                risks = risks == null
                                ? List.of()
                                : List.copyOf(risks);

                model = model == null
                                ? ""
                                : model;

                costUsd = costUsd == null
                                ? BigDecimal.ZERO
                                : costUsd;

                errorType = errorType == null
                                ? ""
                                : errorType;
        }

        public static ModelJudgeResult disabled() {

                return new ModelJudgeResult(
                                ModelJudgeStatus.DISABLED,
                                true,
                                0.0D,
                                0.0D,
                                0.0D,
                                0.0D,
                                0.0D,
                                List.of(),
                                List.of(),
                                "",
                                0,
                                0,
                                0,
                                0,
                                false,
                                false,
                                BigDecimal.ZERO,
                                "");
        }

        public static ModelJudgeResult failed(
                        String errorType,
                        boolean failOpen) {

                return failed(
                                errorType,
                                failOpen,
                                0);
        }

        public static ModelJudgeResult failed(
                        String errorType,
                        boolean failOpen,
                        long durationMs) {

                return new ModelJudgeResult(
                                ModelJudgeStatus.FAILED,
                                failOpen,
                                0.0D,
                                0.0D,
                                0.0D,
                                0.0D,
                                0.0D,
                                List.of(),
                                List.of(),
                                "",
                                Math.max(0, durationMs),
                                0,
                                0,
                                0,
                                false,
                                false,
                                BigDecimal.ZERO,
                                errorType);
        }

        public static ModelJudgeResult skipped() {

                return new ModelJudgeResult(
                                ModelJudgeStatus.SKIPPED,
                                false,
                                0.0D,
                                0.0D,
                                0.0D,
                                0.0D,
                                0.0D,
                                List.of(),
                                List.of(),
                                "",
                                0,
                                0,
                                0,
                                0,
                                false,
                                false,
                                BigDecimal.ZERO,
                                "");
        }

        private static void validateScore(
                        double score,
                        String field) {

                if (Double.isNaN(score)
                                || score < 0.0D
                                || score > 100.0D) {

                        throw new IllegalArgumentException(
                                        field + " must be between 0 and 100");
                }
        }
}
