package com.quince.lawyeraiassistant.agent.runtime.performance;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.agent.performance-guardrail")
public class PerformanceGuardrailProperties {

    private int warnLlmCalls = 12;

    private int criticalLlmCalls = 15;

    private long warnTotalDurationMs = 120_000;

    private long criticalTotalDurationMs = 180_000;

    private long warnLlmDurationMs = 120_000;

    private double warnLlmRatio = 0.95;

    private int warnNoProgressSuppressions = 1;

    private int criticalNoProgressSuppressions = 2;

    private double warnLowCacheHitRatio = 0.20;

    public int getWarnLlmCalls() {
        return warnLlmCalls;
    }

    public void setWarnLlmCalls(
            int warnLlmCalls) {
        this.warnLlmCalls = warnLlmCalls;
    }

    public int getCriticalLlmCalls() {
        return criticalLlmCalls;
    }

    public void setCriticalLlmCalls(
            int criticalLlmCalls) {
        this.criticalLlmCalls = criticalLlmCalls;
    }

    public long getWarnTotalDurationMs() {
        return warnTotalDurationMs;
    }

    public void setWarnTotalDurationMs(
            long warnTotalDurationMs) {
        this.warnTotalDurationMs = warnTotalDurationMs;
    }

    public long getCriticalTotalDurationMs() {
        return criticalTotalDurationMs;
    }

    public void setCriticalTotalDurationMs(
            long criticalTotalDurationMs) {
        this.criticalTotalDurationMs = criticalTotalDurationMs;
    }

    public long getWarnLlmDurationMs() {
        return warnLlmDurationMs;
    }

    public void setWarnLlmDurationMs(
            long warnLlmDurationMs) {
        this.warnLlmDurationMs = warnLlmDurationMs;
    }

    public double getWarnLlmRatio() {
        return warnLlmRatio;
    }

    public void setWarnLlmRatio(
            double warnLlmRatio) {
        this.warnLlmRatio = warnLlmRatio;
    }

    public int getWarnNoProgressSuppressions() {
        return warnNoProgressSuppressions;
    }

    public void setWarnNoProgressSuppressions(
            int warnNoProgressSuppressions) {
        this.warnNoProgressSuppressions = warnNoProgressSuppressions;
    }

    public int getCriticalNoProgressSuppressions() {
        return criticalNoProgressSuppressions;
    }

    public void setCriticalNoProgressSuppressions(
            int criticalNoProgressSuppressions) {
        this.criticalNoProgressSuppressions = criticalNoProgressSuppressions;
    }

    public double getWarnLowCacheHitRatio() {
        return warnLowCacheHitRatio;
    }

    public void setWarnLowCacheHitRatio(
            double warnLowCacheHitRatio) {
        this.warnLowCacheHitRatio = warnLowCacheHitRatio;
    }
}
