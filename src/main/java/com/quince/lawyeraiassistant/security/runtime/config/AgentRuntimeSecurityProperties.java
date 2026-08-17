package com.quince.lawyeraiassistant.security.runtime.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Validated
@ConfigurationProperties(prefix = "agent.runtime")
public class AgentRuntimeSecurityProperties {

    @Positive
    private int maxSteps = 10;

    @Positive
    private int maxToolCalls = 8;

    @Positive
    private int maxReplans = 2;

    @Positive
    private int maxRetries = 3;

    @NotNull
    private Duration maxExecutionTime = Duration.ofSeconds(
            60);

    @NotNull
    private Duration maxToolExecutionTime = Duration.ofSeconds(
            15);

    @Positive
    private int maxObservationLength = 20_000;

    @Positive
    private int maxContextLength = 60_000;

    public int getMaxSteps() {

        return maxSteps;
    }

    public void setMaxSteps(
            int maxSteps) {

        this.maxSteps = maxSteps;
    }

    public int getMaxToolCalls() {

        return maxToolCalls;
    }

    public void setMaxToolCalls(
            int maxToolCalls) {

        this.maxToolCalls = maxToolCalls;
    }

    public int getMaxReplans() {

        return maxReplans;
    }

    public void setMaxReplans(
            int maxReplans) {

        this.maxReplans = maxReplans;
    }

    public int getMaxRetries() {

        return maxRetries;
    }

    public void setMaxRetries(
            int maxRetries) {

        this.maxRetries = maxRetries;
    }

    public Duration getMaxExecutionTime() {

        return maxExecutionTime;
    }

    public void setMaxExecutionTime(
            Duration maxExecutionTime) {

        this.maxExecutionTime = maxExecutionTime;
    }

    public Duration getMaxToolExecutionTime() {

        return maxToolExecutionTime;
    }

    public void setMaxToolExecutionTime(
            Duration maxToolExecutionTime) {

        this.maxToolExecutionTime = maxToolExecutionTime;
    }

    public int getMaxObservationLength() {

        return maxObservationLength;
    }

    public void setMaxObservationLength(
            int maxObservationLength) {

        this.maxObservationLength = maxObservationLength;
    }

    public int getMaxContextLength() {

        return maxContextLength;
    }

    public void setMaxContextLength(
            int maxContextLength) {

        this.maxContextLength = maxContextLength;
    }

    @jakarta.validation.constraints.AssertTrue(message = "runtime durations must be greater than zero")
    public boolean isDurationConfigurationValid() {

        return maxExecutionTime != null
                && !maxExecutionTime.isZero()
                && !maxExecutionTime.isNegative()
                && maxToolExecutionTime != null
                && !maxToolExecutionTime.isZero()
                && !maxToolExecutionTime.isNegative();
    }
}