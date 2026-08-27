package com.quince.lawyeraiassistant.evaluation.judge;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.evaluation.judge")
public class ModelJudgeProperties {

    private boolean enabled = false;

    private double minimumScore = 75.0D;

    private boolean failOpen = true;

    private int maximumAnswerCharacters = 12_000;

    public boolean isEnabled() {

        return enabled;
    }

    public void setEnabled(
            boolean enabled) {

        this.enabled = enabled;
    }

    public double getMinimumScore() {

        return minimumScore;
    }

    public void setMinimumScore(
            double minimumScore) {

        if (Double.isNaN(minimumScore)
                || minimumScore < 0.0D
                || minimumScore > 100.0D) {

            throw new IllegalArgumentException(
                    "minimumScore must be between 0 and 100");
        }

        this.minimumScore = minimumScore;
    }

    public boolean isFailOpen() {

        return failOpen;
    }

    public void setFailOpen(
            boolean failOpen) {

        this.failOpen = failOpen;
    }

    public int getMaximumAnswerCharacters() {

        return maximumAnswerCharacters;
    }

    public void setMaximumAnswerCharacters(
            int maximumAnswerCharacters) {

        if (maximumAnswerCharacters <= 0) {
            throw new IllegalArgumentException(
                    "maximumAnswerCharacters must be greater than zero");
        }

        this.maximumAnswerCharacters =
                maximumAnswerCharacters;
    }
}