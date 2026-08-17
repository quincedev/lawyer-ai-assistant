package com.quince.lawyeraiassistant.security.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import com.quince.lawyeraiassistant.security.SecurityTest;

@SecurityTest
class AgentExecutionLimitsTest {

    @Test
    void shouldCreateValidLimits() {

        AgentExecutionLimits limits = new AgentExecutionLimits(
                10,
                5,
                2,
                2,
                Duration.ofSeconds(60),
                Duration.ofSeconds(10),
                20_000,
                60_000);
        ;

        assertEquals(
                10,
                limits.maxSteps());

        assertEquals(
                5,
                limits.maxToolCalls());

        assertEquals(
                2,
                limits.maxReplans());

        assertEquals(
                2,
                limits.maxRetries());

        assertEquals(
                Duration.ofSeconds(60),
                limits.maxExecutionTime());

        assertEquals(
                20_000,
                limits.maxObservationLength());

        assertEquals(
                60_000,
                limits.maxContextLength());
    }

    @Test
    void shouldAllowZeroReplansAndRetries() {

        AgentExecutionLimits limits = new AgentExecutionLimits(
                1,
                1,
                0,
                0,
                Duration.ofSeconds(1),
                Duration.ofSeconds(10),
                1,
                1);

        assertEquals(
                0,
                limits.maxReplans());

        assertEquals(
                0,
                limits.maxRetries());
    }

    @Test
    void shouldRejectZeroMaxSteps() {

        assertThrows(
                IllegalArgumentException.class,
                () -> createLimits(
                        0,
                        8,
                        2,
                        2));
    }

    @Test
    void shouldRejectZeroMaxToolCalls() {

        assertThrows(
                IllegalArgumentException.class,
                () -> createLimits(
                        12,
                        0,
                        2,
                        2));
    }

    @Test
    void shouldRejectNegativeMaxReplans() {

        assertThrows(
                IllegalArgumentException.class,
                () -> createLimits(
                        12,
                        8,
                        -1,
                        2));
    }

    @Test
    void shouldRejectNegativeMaxRetries() {

        assertThrows(
                IllegalArgumentException.class,
                () -> createLimits(
                        12,
                        8,
                        2,
                        -1));
    }

    @Test
    void shouldRejectZeroToolExecutionTime() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new AgentExecutionLimits(
                        10,
                        8,
                        2,
                        3,
                        Duration.ofSeconds(120),
                        Duration.ZERO,
                        20_000,
                        60_000));
    }

    @Test
    void shouldRejectZeroObservationLength() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new AgentExecutionLimits(
                        10,
                        8,
                        2,
                        3,
                        Duration.ofSeconds(120),
                        Duration.ofSeconds(30),
                        0,
                        60_000));
    }

    @Test
    void shouldRejectZeroContextLength() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new AgentExecutionLimits(
                        10,
                        8,
                        2,
                        3,
                        Duration.ofSeconds(120),
                        Duration.ofSeconds(30),
                        20_000,
                        0));
    }

    private AgentExecutionLimits createLimits(
            int maxSteps,
            int maxToolCalls,
            int maxReplans,
            int maxRetries) {

        return new AgentExecutionLimits(
                maxSteps,
                maxToolCalls,
                maxReplans,
                maxRetries,
                Duration.ofSeconds(60),
                Duration.ofSeconds(10),
                20_000,
                60_000);
    }
}