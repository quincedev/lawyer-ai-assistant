package com.quince.lawyeraiassistant.security.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import com.quince.lawyeraiassistant.security.SecurityTest;

@SecurityTest
class AgentExecutionBudgetTest {

    @Test
    void shouldStartWithZeroUsage() {

        AgentExecutionBudget budget = new AgentExecutionBudget(
                createLimits());

        assertEquals(
                0,
                budget.stepsUsed());

        assertEquals(
                0,
                budget.toolCallsUsed());

        assertEquals(
                0,
                budget.replansUsed());

        assertEquals(
                0,
                budget.retriesUsed());
    }

    @Test
    void shouldRecordRuntimeUsage() {

        AgentExecutionBudget budget = new AgentExecutionBudget(
                createLimits());

        budget.recordStep();
        budget.recordStep();

        budget.recordToolCall();

        budget.recordReplan();

        budget.recordRetry();
        budget.recordRetry();

        assertEquals(
                2,
                budget.stepsUsed());

        assertEquals(
                1,
                budget.toolCallsUsed());

        assertEquals(
                1,
                budget.replansUsed());

        assertEquals(
                2,
                budget.retriesUsed());
    }

    @Test
    void shouldCalculateElapsedTimeUsingClock() {

        Instant start = Instant.parse(
                "2026-08-16T00:00:00Z");

        Clock clock = Clock.fixed(
                start,
                ZoneOffset.UTC);

        AgentExecutionBudget budget = new AgentExecutionBudget(
                createLimits(),
                clock);

        assertEquals(
                Duration.ZERO,
                budget.elapsed());

        assertEquals(
                start,
                budget.startedAt());
    }

    private AgentExecutionLimits createLimits() {

        return new AgentExecutionLimits(
                12,
                8,
                2,
                2,
                Duration.ofSeconds(60),
                Duration.ofSeconds(10),
                20_000,
                60_000);
    }
}