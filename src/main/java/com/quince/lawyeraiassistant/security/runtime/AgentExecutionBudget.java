package com.quince.lawyeraiassistant.security.runtime;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Mutable resource consumption state for a single Agent execution.
 *
 * <p>
 * One budget instance belongs to exactly one Agent execution.
 * It must never be shared between independent Agent executions.
 * </p>
 */
public final class AgentExecutionBudget {

    private final AgentExecutionLimits limits;

    private final Clock clock;

    private final Instant startedAt;

    private int stepsUsed;

    private int toolCallsUsed;

    private int replansUsed;

    private int retriesUsed;

    public AgentExecutionBudget(
            AgentExecutionLimits limits) {

        this(
                limits,
                Clock.systemUTC());
    }

    AgentExecutionBudget(
            AgentExecutionLimits limits,
            Clock clock) {

        this.limits = Objects.requireNonNull(
                limits,
                "limits must not be null");

        this.clock = Objects.requireNonNull(
                clock,
                "clock must not be null");

        this.startedAt = clock.instant();
    }

    public void recordStep() {

        stepsUsed++;
    }

    public void recordToolCall() {

        toolCallsUsed++;
    }

    public void recordReplan() {

        replansUsed++;
    }

    public void recordRetry() {

        retriesUsed++;
    }

    public int stepsUsed() {

        return stepsUsed;
    }

    public int toolCallsUsed() {

        return toolCallsUsed;
    }

    public int replansUsed() {

        return replansUsed;
    }

    public int retriesUsed() {

        return retriesUsed;
    }

    public Instant startedAt() {

        return startedAt;
    }

    public Duration elapsed() {

        return Duration.between(
                startedAt,
                clock.instant());
    }

    public AgentExecutionLimits limits() {

        return limits;
    }
}