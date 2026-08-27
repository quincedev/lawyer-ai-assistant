package com.quince.lawyeraiassistant.agent.runtime.metrics;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class AgentPerformanceSnapshotRecorder {

    private final AtomicReference<AgentPerformanceSnapshot> latest = new AtomicReference<>();

    /*
     * Live Evaluation 使用请求线程绑定的 Snapshot，
     * 避免被其他并发 Agent 请求覆盖。
     */
    private final ThreadLocal<AgentPerformanceSnapshot> currentThreadLatest = new ThreadLocal<>();

    public void record(
            AgentPerformanceSnapshot snapshot) {

        if (snapshot == null) {
            return;
        }

        latest.set(snapshot);

        currentThreadLatest.set(snapshot);
    }

    public Optional<AgentPerformanceSnapshot> latest() {

        return Optional.ofNullable(
                latest.get());
    }

    public AgentPerformanceSnapshot requireLatest() {

        AgentPerformanceSnapshot snapshot = latest.get();

        if (snapshot == null) {

            throw new IllegalStateException(
                    "No completed Agent performance snapshot is available");
        }

        return snapshot;
    }

    public Optional<AgentPerformanceSnapshot> currentThreadLatest() {

        return Optional.ofNullable(
                currentThreadLatest.get());
    }

    public AgentPerformanceSnapshot requireCurrentThreadLatest() {

        AgentPerformanceSnapshot snapshot = currentThreadLatest.get();

        if (snapshot == null) {

            throw new IllegalStateException(
                    "No completed Agent performance snapshot "
                            + "is available for the current thread");
        }

        return snapshot;
    }

    public void clearCurrentThread() {

        currentThreadLatest.remove();
    }

    public void clear() {

        latest.set(null);

        currentThreadLatest.remove();
    }
}