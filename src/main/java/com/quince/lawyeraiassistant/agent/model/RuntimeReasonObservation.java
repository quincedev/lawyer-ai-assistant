package com.quince.lawyeraiassistant.agent.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

/**
 * Agent Runtime Reason 阶段产生的中间分析结果。
 *
 * <p>
 * 与 Initial ReasonResult 不同：
 * </p>
 *
 * <ul>
 * <li>ReasonResult：理解用户原始 Goal</li>
 * <li>RuntimeReasonObservation：执行某个 Task 时产生的阶段性分析</li>
 * </ul>
 */
@Getter
@ToString
@EqualsAndHashCode
public final class RuntimeReasonObservation {

    private final String taskId;

    private final String content;

    private RuntimeReasonObservation(
            String taskId,
            String content) {

        this.taskId = normalize(
                taskId,
                "Task id must not be blank");

        this.content = normalize(
                content,
                "Runtime reason content must not be blank");
    }

    public static RuntimeReasonObservation of(
            String taskId,
            String content) {

        return new RuntimeReasonObservation(
                taskId,
                content);
    }

    private static String normalize(
            String value,
            String message) {

        Objects.requireNonNull(
                value,
                message);

        String normalized = value.trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    message);
        }

        return normalized;
    }
}