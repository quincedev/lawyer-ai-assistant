package com.quince.lawyeraiassistant.performance;

import java.util.concurrent.TimeUnit;

public final class PerformanceTimer {

    private final long startedAt;

    private PerformanceTimer() {

        this.startedAt = System.nanoTime();
    }

    public static PerformanceTimer start() {

        return new PerformanceTimer();
    }

    public long elapsedMillis() {

        return TimeUnit.NANOSECONDS
                .toMillis(
                        System.nanoTime()
                                - startedAt);
    }
}