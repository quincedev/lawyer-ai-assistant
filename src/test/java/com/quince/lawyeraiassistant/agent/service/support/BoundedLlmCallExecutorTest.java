package com.quince.lawyeraiassistant.agent.service.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class BoundedLlmCallExecutorTest {

    private final BoundedLlmCallExecutor executor = new BoundedLlmCallExecutor();

    @Test
    void shouldReturnImmediatelyWhenFirstAttemptSucceeds() {

        AtomicInteger attempts = new AtomicInteger();

        String result = executor.execute(
                "TEST",
                () -> {

                    attempts.incrementAndGet();

                    return "ok";
                });

        assertEquals(
                "ok",
                result);

        assertEquals(
                1,
                attempts.get());
    }

    @Test
    void shouldRetryOnceWhenFirstAttemptIsInvalid() {

        AtomicInteger attempts = new AtomicInteger();

        String result = executor.execute(
                "TEST",
                () -> {

                    int current = attempts.incrementAndGet();

                    if (current == 1) {

                        throw new RetryableLlmResponseException(
                                "blank response");
                    }

                    return "ok";
                });

        assertEquals(
                "ok",
                result);

        assertEquals(
                2,
                attempts.get());
    }

    @Test
    void shouldFailAfterSecondInvalidAttempt() {

        AtomicInteger attempts = new AtomicInteger();

        RetryableLlmResponseException exception = assertThrows(
                RetryableLlmResponseException.class,
                () -> executor.execute(
                        "TEST",
                        () -> {

                            attempts.incrementAndGet();

                            throw new RetryableLlmResponseException(
                                    "blank response");
                        }));

        assertEquals(
                "blank response",
                exception.getMessage());

        assertEquals(
                2,
                attempts.get());
    }

    @Test
    void shouldNotRetryNonRetryableRuntimeException() {

        AtomicInteger attempts = new AtomicInteger();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> executor.execute(
                        "TEST",
                        () -> {

                            attempts.incrementAndGet();

                            throw new IllegalArgumentException(
                                    "programming error");
                        }));

        assertEquals(
                "programming error",
                exception.getMessage());

        assertEquals(
                1,
                attempts.get());
    }
}