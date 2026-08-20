package com.quince.lawyeraiassistant.agent.runtime.performance.regression;

public record PerformanceRegressionMetricResult(

        String metric,

        PerformanceRegressionLevel level,

        double actual,

        double baseline,

        String reason) {

    public boolean isPass() {
        return level == PerformanceRegressionLevel.PASS;
    }

    public boolean isWarn() {
        return level == PerformanceRegressionLevel.WARN;
    }

    public boolean isFail() {
        return level == PerformanceRegressionLevel.FAIL;
    }
}