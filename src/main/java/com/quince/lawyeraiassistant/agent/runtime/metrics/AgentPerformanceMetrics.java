package com.quince.lawyeraiassistant.agent.runtime.metrics;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Per-execution Agent performance metrics.
 *
 * 一个 Agent Execution 创建一个实例。
 *
 * 用于记录：
 * - Agent 总耗时
 * - LLM 调用
 * - Tool 调用
 * - MCP 调用
 * - Cache HIT/MISS
 * - Evidence compaction
 * - Retry / No-progress suppression
 */
public final class AgentPerformanceMetrics {

    private final long startedAtNanos = System.nanoTime();

    private final AtomicInteger llmCalls = new AtomicInteger();

    private final AtomicLong llmDurationMs = new AtomicLong();

    private final AtomicInteger toolCalls = new AtomicInteger();

    private final AtomicLong toolDurationMs = new AtomicLong();

    private final AtomicInteger mcpCalls = new AtomicInteger();

    private final AtomicLong mcpDurationMs = new AtomicLong();

    private final AtomicInteger cacheHits = new AtomicInteger();

    private final AtomicInteger cacheMisses = new AtomicInteger();

    private final AtomicLong evidenceOriginalChars = new AtomicLong();

    private final AtomicLong evidenceCompactedChars = new AtomicLong();

    private final AtomicInteger retries = new AtomicInteger();

    private final AtomicInteger noProgressSuppressions = new AtomicInteger();

    /*
     * =================================================
     * LLM
     * =================================================
     */

    public void recordLlmCall(
            long durationMs) {

        llmCalls.incrementAndGet();

        llmDurationMs.addAndGet(
                Math.max(
                        0,
                        durationMs));
    }

    /*
     * =================================================
     * TOOL
     * =================================================
     */

    public void recordToolCall(
            long durationMs) {

        toolCalls.incrementAndGet();

        toolDurationMs.addAndGet(
                Math.max(
                        0,
                        durationMs));
    }

    /*
     * =================================================
     * MCP
     * =================================================
     */

    public void recordMcpCall(
            long durationMs) {

        mcpCalls.incrementAndGet();

        mcpDurationMs.addAndGet(
                Math.max(
                        0,
                        durationMs));
    }

    /*
     * =================================================
     * CACHE
     * =================================================
     */

    public void recordCacheHit() {

        cacheHits.incrementAndGet();
    }

    public void recordCacheMiss() {

        cacheMisses.incrementAndGet();
    }

    /*
     * =================================================
     * EVIDENCE
     * =================================================
     */

    public void recordEvidenceCompaction(
            long originalChars,
            long compactedChars) {

        evidenceOriginalChars.addAndGet(
                Math.max(
                        0,
                        originalChars));

        evidenceCompactedChars.addAndGet(
                Math.max(
                        0,
                        compactedChars));
    }

    /*
     * =================================================
     * RETRY
     * =================================================
     */

    public void recordRetry() {

        retries.incrementAndGet();
    }

    public void recordNoProgressSuppression() {

        noProgressSuppressions.incrementAndGet();
    }

    /*
     * =================================================
     * SNAPSHOT
     * =================================================
     */

    public AgentPerformanceSnapshot snapshot() {

        long totalDurationMs = (System.nanoTime()
                - startedAtNanos)
                / 1_000_000;

        return new AgentPerformanceSnapshot(
                totalDurationMs,

                llmCalls.get(),
                llmDurationMs.get(),

                toolCalls.get(),
                toolDurationMs.get(),

                mcpCalls.get(),
                mcpDurationMs.get(),

                cacheHits.get(),
                cacheMisses.get(),

                evidenceOriginalChars.get(),
                evidenceCompactedChars.get(),

                retries.get(),
                noProgressSuppressions.get());
    }
}