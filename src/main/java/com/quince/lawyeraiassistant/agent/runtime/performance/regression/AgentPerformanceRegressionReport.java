package com.quince.lawyeraiassistant.agent.runtime.performance.regression;

import java.util.List;

public record AgentPerformanceRegressionReport(

        PerformanceRegressionLevel overallLevel,

        List<PerformanceRegressionMetricResult> results) {

    public AgentPerformanceRegressionReport {

        results = results == null
                ? List.of()
                : List.copyOf(results);
    }

    public boolean passed() {

        return overallLevel != PerformanceRegressionLevel.FAIL;
    }

    public boolean hasWarnings() {

        return results.stream()
                .anyMatch(
                        PerformanceRegressionMetricResult::isWarn);
    }

    public boolean hasFailures() {

        return results.stream()
                .anyMatch(
                        PerformanceRegressionMetricResult::isFail);
    }
}