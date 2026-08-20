package com.quince.lawyeraiassistant.agent.service.support;

import java.util.Objects;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Agent LLM Stage 的 bounded retry 执行器。
 *
 * <p>
 * 当前策略：
 * </p>
 *
 * <pre>
 * Attempt 1
 *   ↓ invalid response
 * Attempt 2
 *   ↓
 * success / fail
 * </pre>
 *
 * 最多调用两次，即只补偿重试一次。
 */
@Component
public class BoundedLlmCallExecutor {

    private static final Logger log = LoggerFactory.getLogger(
            BoundedLlmCallExecutor.class);

    /**
     * 初始请求 + 1 次补偿重试。
     */
    private static final int MAX_ATTEMPTS = 2;

    public <T> T execute(
            String stage,
            Supplier<T> supplier) {

        Objects.requireNonNull(
                stage,
                "stage must not be null");

        Objects.requireNonNull(
                supplier,
                "supplier must not be null");

        RetryableLlmResponseException lastException = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {

            try {

                return supplier.get();

            } catch (RetryableLlmResponseException exception) {

                lastException = exception;

                if (attempt >= MAX_ATTEMPTS) {

                    log.error(
                            "LLM stage failed after bounded retry. stage={}, attempts={}, reason={}",
                            stage,
                            attempt,
                            exception.getMessage());

                    throw exception;
                }

                log.warn(
                        "Retryable LLM response detected. stage={}, attempt={}, maxAttempts={}, reason={}",
                        stage,
                        attempt,
                        MAX_ATTEMPTS,
                        exception.getMessage());
            }
        }

        /*
         * 正常情况下不会进入这里。
         */
        throw Objects.requireNonNull(
                lastException,
                "Retryable LLM exception must exist");
    }
}