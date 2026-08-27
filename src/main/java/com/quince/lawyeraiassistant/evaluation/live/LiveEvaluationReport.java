package com.quince.lawyeraiassistant.evaluation.live;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record LiveEvaluationReport(
                String runId,
                Instant startedAt,
                Instant completedAt,
                int totalCases,
                int passedCases,
                int failedCases,
                double passRate,
                double averageScore,
                long totalPromptTokens,
                long totalCompletionTokens,
                long totalTokens,
                BigDecimal totalCostUsd,
                List<LiveEvaluationCaseReport> cases) {

        public LiveEvaluationReport {

                cases = cases == null
                                ? List.of()
                                : List.copyOf(cases);

                totalCostUsd = totalCostUsd == null
                                ? BigDecimal.ZERO
                                : totalCostUsd;
        }
}