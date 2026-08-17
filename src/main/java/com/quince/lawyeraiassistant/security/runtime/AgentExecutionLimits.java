package com.quince.lawyeraiassistant.security.runtime;

import java.time.Duration;
import java.util.Objects;

public record AgentExecutionLimits(
        int maxSteps,
        int maxToolCalls,
        int maxReplans,
        int maxRetries,
        Duration maxExecutionTime,
        Duration maxToolExecutionTime,
        int maxObservationLength,
        int maxContextLength) {

    public AgentExecutionLimits {

        if (maxSteps <= 0) {
            throw new IllegalArgumentException(
                    "maxSteps must be greater than 0");
        }

        if (maxToolCalls <= 0) {
            throw new IllegalArgumentException(
                    "maxToolCalls must be greater than 0");
        }

        if (maxReplans < 0) {
            throw new IllegalArgumentException(
                    "maxReplans must not be negative");
        }

        if (maxRetries < 0) {
            throw new IllegalArgumentException(
                    "maxRetries must not be negative");
        }

        Objects.requireNonNull(
                maxExecutionTime,
                "maxExecutionTime must not be null");

        if (maxExecutionTime.isZero()
                || maxExecutionTime.isNegative()) {

            throw new IllegalArgumentException(
                    "maxExecutionTime must be greater than 0");
        }

        Objects.requireNonNull(
                maxToolExecutionTime,
                "maxToolExecutionTime must not be null");

        if (maxToolExecutionTime.isZero()
                || maxToolExecutionTime.isNegative()) {

            throw new IllegalArgumentException(
                    "maxToolExecutionTime must be greater than 0");
        }

        if (maxObservationLength <= 0) {
            throw new IllegalArgumentException(
                    "maxObservationLength must be greater than 0");
        }

        if (maxContextLength <= 0) {
            throw new IllegalArgumentException(
                    "maxContextLength must be greater than 0");
        }
    }
}