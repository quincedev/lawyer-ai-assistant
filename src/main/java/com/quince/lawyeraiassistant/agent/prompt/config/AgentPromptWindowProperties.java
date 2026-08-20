package com.quince.lawyeraiassistant.agent.prompt.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

@Validated
@ConfigurationProperties(prefix = "app.agent.prompt-window")
public class AgentPromptWindowProperties {

    /**
     * Action Selection 最多携带多少条其他 Task 的历史 Observation。
     */
    @Min(0)
    private int maxHistoricalObservations = 1;

    /**
     * 单条 Evidence 在 Prompt 中允许暴露的最大字符数。
     *
     * Runtime 中仍保存完整 Evidence，
     * 这里只控制 Model-visible Prompt View。
     */
    @Positive
    private int maxEvidenceChars = 4000;

    public int getMaxHistoricalObservations() {

        return maxHistoricalObservations;
    }

    public void setMaxHistoricalObservations(
            int maxHistoricalObservations) {

        this.maxHistoricalObservations = maxHistoricalObservations;
    }

    public int getMaxEvidenceChars() {

        return maxEvidenceChars;
    }

    public void setMaxEvidenceChars(
            int maxEvidenceChars) {

        this.maxEvidenceChars = maxEvidenceChars;
    }
}