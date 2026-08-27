package com.quince.lawyeraiassistant.agent.runtime.performance;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "app.agent.performance-guardrail")
public class PerformanceGuardrailProperties {

    private int warnLlmCalls = 12;

    private int criticalLlmCalls = 15;

    private long warnTotalDurationMs = 120_000;

    private long criticalTotalDurationMs = 180_000;

    private long warnLlmDurationMs = 120_000;

    private double warnLlmRatio = 0.95D;

    private int warnNoProgressSuppressions = 1;

    private int criticalNoProgressSuppressions = 2;

    private double warnLowCacheHitRatio = 0.20D;

    /**
     * 是否启用执行级成本 Guardrail。
     */
    private boolean costEnabled = true;

    /**
     * 单次 Agent Execution 成本达到该值时发出 WARN。
     */
    private BigDecimal warnExecutionCostUsd = new BigDecimal("0.02");

    /**
     * 单次 Agent Execution 成本达到该值时发出 CRITICAL。
     */
    private BigDecimal criticalExecutionCostUsd = new BigDecimal("0.05");

    /**
     * Provider 没有返回完整 Usage 时是否发出 CRITICAL。
     */
    private boolean requireCompleteTokenUsage = true;

    /**
     * Usage 存在但找不到模型价格时是否发出 CRITICAL。
     */
    private boolean requireCompletePricing = true;

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

    public boolean isCostEnabled() {

        return costEnabled;
    }

    public void setCostEnabled(
            boolean costEnabled) {

        this.costEnabled = costEnabled;
    }

    public BigDecimal getWarnExecutionCostUsd() {

        return warnExecutionCostUsd;
    }

    public void setWarnExecutionCostUsd(
            BigDecimal warnExecutionCostUsd) {

        this.warnExecutionCostUsd = warnExecutionCostUsd;
    }

    public BigDecimal getCriticalExecutionCostUsd() {

        return criticalExecutionCostUsd;
    }

    public void setCriticalExecutionCostUsd(
            BigDecimal criticalExecutionCostUsd) {

        this.criticalExecutionCostUsd = criticalExecutionCostUsd;
    }

    public boolean isRequireCompleteTokenUsage() {

        return requireCompleteTokenUsage;
    }

    public void setRequireCompleteTokenUsage(
            boolean requireCompleteTokenUsage) {

        this.requireCompleteTokenUsage = requireCompleteTokenUsage;
    }

    public boolean isRequireCompletePricing() {

        return requireCompletePricing;
    }

    public void setRequireCompletePricing(
            boolean requireCompletePricing) {

        this.requireCompletePricing = requireCompletePricing;
    }
}