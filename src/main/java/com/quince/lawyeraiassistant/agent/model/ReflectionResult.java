package com.quince.lawyeraiassistant.agent.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

@Getter
@ToString
@EqualsAndHashCode
public final class ReflectionResult {

    private final ReflectionDecision decision;

    private final String summary;

    private ReflectionResult(
            ReflectionDecision decision,
            String summary) {

        this.decision = Objects.requireNonNull(
                decision,
                "ReflectionDecision must not be null");

        this.summary = normalizeSummary(
                summary);
    }

    public static ReflectionResult of(
            ReflectionDecision decision,
            String summary) {

        return new ReflectionResult(
                decision,
                summary);
    }

    public boolean shouldContinue() {
        return decision == ReflectionDecision.CONTINUE;
    }

    public boolean shouldRetry() {
        return decision == ReflectionDecision.RETRY;
    }

    public boolean shouldReplan() {
        return decision == ReflectionDecision.REPLAN;
    }

    public boolean shouldFinish() {
        return decision == ReflectionDecision.FINISH;
    }

    private static String normalizeSummary(
            String summary) {

        Objects.requireNonNull(
                summary,
                "Reflection summary must not be null");

        String normalized = summary.trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    "Reflection summary must not be blank");
        }

        return normalized;
    }
}