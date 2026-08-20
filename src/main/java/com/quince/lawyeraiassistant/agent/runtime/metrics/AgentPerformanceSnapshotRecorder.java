package com.quince.lawyeraiassistant.agent.runtime.metrics;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

/**
 * 保存最近一次完成的 Agent Performance Snapshot。
 *
 * 第一版主要用于：
 * - Performance Regression E2E
 * - Release Gate
 * - 本地诊断
 *
 * 注意：
 * 这不是长期生产历史存储。
 */
@Component
public class AgentPerformanceSnapshotRecorder {

    private final AtomicReference<AgentPerformanceSnapshot> latest = new AtomicReference<>();

    public void record(
            AgentPerformanceSnapshot snapshot) {

        if (snapshot == null) {
            return;
        }

        latest.set(
                snapshot);
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

    public void clear() {

        latest.set(
                null);
    }
}