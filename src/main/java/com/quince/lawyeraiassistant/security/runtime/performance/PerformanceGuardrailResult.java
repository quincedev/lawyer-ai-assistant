package com.quince.lawyeraiassistant.security.runtime.performance;

public record PerformanceGuardrailResult(
        PerformanceGuardrailLevel level,
        String metric,
        double actual,
        double threshold,
        String reason) {

    public boolean isOk() {

        return level == PerformanceGuardrailLevel.OK;
    }

    public boolean isWarn() {

        return level == PerformanceGuardrailLevel.WARN;
    }

    public boolean isCritical() {

        return level == PerformanceGuardrailLevel.CRITICAL;
    }

    public static PerformanceGuardrailResult ok(
            String metric,
            double actual) {

        return new PerformanceGuardrailResult(
                PerformanceGuardrailLevel.OK,
                metric,
                actual,
                -1,
                "Within performance threshold");
    }

    public static PerformanceGuardrailResult warn(
            String metric,
            double actual,
            double threshold,
            String reason) {

        return new PerformanceGuardrailResult(
                PerformanceGuardrailLevel.WARN,
                metric,
                actual,
                threshold,
                reason);
    }

    public static PerformanceGuardrailResult critical(
            String metric,
            double actual,
            double threshold,
            String reason) {

        return new PerformanceGuardrailResult(
                PerformanceGuardrailLevel.CRITICAL,
                metric,
                actual,
                threshold,
                reason);
    }
}
