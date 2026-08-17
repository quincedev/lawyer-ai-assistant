package com.quince.lawyeraiassistant.security.runtime;

import com.quince.lawyeraiassistant.security.runtime.policy.ExecutionTimeRuntimeGuardrailPolicy;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertTrue;
import com.quince.lawyeraiassistant.security.SecurityTest;

@SecurityTest
class ExecutionTimeRuntimeGuardrailPolicyTest {

    @Test
    void shouldAllowWhenExecutionTimeRemains() {

        MutableClock clock = new MutableClock(
                Instant.parse(
                        "2026-08-16T00:00:00Z"),
                ZoneOffset.UTC);

        AgentExecutionBudget budget = new AgentExecutionBudget(
                createLimits(),
                clock);

        clock.advance(
                Duration.ofSeconds(59));

        RuntimeGuardrailResult result = new ExecutionTimeRuntimeGuardrailPolicy()
                .evaluate(
                        RuntimeGuardrailOperation.STEP,
                        budget);

        assertTrue(
                result.isAllowed());
    }

    @Test
    void shouldDenyWhenExecutionTimeExactlyReached() {

        MutableClock clock = new MutableClock(
                Instant.parse(
                        "2026-08-16T00:00:00Z"),
                ZoneOffset.UTC);

        AgentExecutionBudget budget = new AgentExecutionBudget(
                createLimits(),
                clock);

        clock.advance(
                Duration.ofSeconds(60));

        RuntimeGuardrailResult result = new ExecutionTimeRuntimeGuardrailPolicy()
                .evaluate(
                        RuntimeGuardrailOperation.STEP,
                        budget);

        assertTrue(
                result.isDenied());
    }

    @Test
    void shouldDenyWhenExecutionTimeExceeded() {

        MutableClock clock = new MutableClock(
                Instant.parse(
                        "2026-08-16T00:00:00Z"),
                ZoneOffset.UTC);

        AgentExecutionBudget budget = new AgentExecutionBudget(
                createLimits(),
                clock);

        clock.advance(
                Duration.ofSeconds(61));

        RuntimeGuardrailResult result = new ExecutionTimeRuntimeGuardrailPolicy()
                .evaluate(
                        RuntimeGuardrailOperation.REPLAN,
                        budget);

        assertTrue(
                result.isDenied());
    }

    private AgentExecutionLimits createLimits() {

        return new AgentExecutionLimits(
                10,
                5,
                2,
                2,
                Duration.ofSeconds(60),
                Duration.ofSeconds(10),
                20_000,
                60_000);
    }

    private static final class MutableClock
            extends Clock {

        private Instant instant;

        private final ZoneId zone;

        private MutableClock(
                Instant instant,
                ZoneId zone) {

            this.instant = instant;
            this.zone = zone;
        }

        void advance(
                Duration duration) {

            instant = instant.plus(
                    duration);
        }

        @Override
        public ZoneId getZone() {

            return zone;
        }

        @Override
        public Clock withZone(
                ZoneId zone) {

            return new MutableClock(
                    instant,
                    zone);
        }

        @Override
        public Instant instant() {

            return instant;
        }
    }
}