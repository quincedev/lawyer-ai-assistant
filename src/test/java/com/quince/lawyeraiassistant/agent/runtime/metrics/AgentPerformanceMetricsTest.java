package com.quince.lawyeraiassistant.agent.runtime.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AgentPerformanceMetricsTest {

        @Test
        void shouldAggregatePerformanceMetricsAndTokenUsage() {

                AgentPerformanceMetrics metrics = new AgentPerformanceMetrics();

                /*
                 * Three LLM calls:
                 *
                 * Call 1 → complete usage
                 * Call 2 → complete usage
                 * Call 3 → usage unavailable
                 */
                metrics.recordLlmCall(
                                100);

                metrics.recordTokenUsage(
                                AgentTokenUsage.available(
                                                1_000,
                                                200,
                                                1_200));

                metrics.recordLlmCall(
                                200);

                metrics.recordTokenUsage(
                                AgentTokenUsage.available(
                                                500,
                                                100,
                                                600));

                metrics.recordLlmCall(
                                50);

                metrics.recordTokenUsage(
                                AgentTokenUsage.unavailable());

                metrics.recordToolCall(
                                50);

                metrics.recordMcpCall(
                                40);

                metrics.recordCacheHit();
                metrics.recordCacheMiss();

                metrics.recordEvidenceCompaction(
                                10_000,
                                4_000);

                metrics.recordRetry();

                metrics.recordNoProgressSuppression();

                AgentPerformanceSnapshot snapshot = metrics.snapshot();

                /*
                 * =====================================================
                 * LLM
                 * =====================================================
                 */

                assertEquals(
                                3,
                                snapshot.llmCalls());

                assertEquals(
                                350,
                                snapshot.llmDurationMs());

                /*
                 * =====================================================
                 * Token Usage
                 * =====================================================
                 */

                assertEquals(
                                1_500,
                                snapshot.promptTokens());

                assertEquals(
                                300,
                                snapshot.completionTokens());

                assertEquals(
                                1_800,
                                snapshot.totalTokens());

                assertEquals(
                                1,
                                snapshot.usageMissingCalls());

                assertEquals(
                                2.0 / 3.0,
                                snapshot.tokenUsageCoverageRatio(),
                                0.001);

                assertFalse(
                                snapshot.hasCompleteTokenUsage());

                /*
                 * =====================================================
                 * Tool
                 * =====================================================
                 */

                assertEquals(
                                1,
                                snapshot.toolCalls());

                assertEquals(
                                50,
                                snapshot.toolDurationMs());

                /*
                 * =====================================================
                 * MCP
                 * =====================================================
                 */

                assertEquals(
                                1,
                                snapshot.mcpCalls());

                assertEquals(
                                40,
                                snapshot.mcpDurationMs());

                /*
                 * =====================================================
                 * Cache
                 * =====================================================
                 */

                assertEquals(
                                1,
                                snapshot.cacheHits());

                assertEquals(
                                1,
                                snapshot.cacheMisses());

                assertEquals(
                                0.5,
                                snapshot.cacheHitRatio(),
                                0.001);

                /*
                 * =====================================================
                 * Evidence
                 * =====================================================
                 */

                assertEquals(
                                10_000,
                                snapshot.evidenceOriginalChars());

                assertEquals(
                                4_000,
                                snapshot.evidenceCompactedChars());

                assertEquals(
                                0.6,
                                snapshot.evidenceReductionRatio(),
                                0.001);

                /*
                 * =====================================================
                 * Retry / No Progress
                 * =====================================================
                 */

                assertEquals(
                                1,
                                snapshot.retries());

                assertEquals(
                                1,
                                snapshot.noProgressSuppressions());

                /*
                 * =====================================================
                 * Total Duration
                 * =====================================================
                 */

                assertTrue(
                                snapshot.totalDurationMs() >= 0);
        }

        @Test
        void shouldAggregateCompleteTokenUsageAcrossLlmCalls() {

                AgentPerformanceMetrics metrics = new AgentPerformanceMetrics();

                metrics.recordLlmCall(
                                120);

                metrics.recordTokenUsage(
                                AgentTokenUsage.available(
                                                800,
                                                200,
                                                1_000));

                metrics.recordLlmCall(
                                180);

                metrics.recordTokenUsage(
                                AgentTokenUsage.available(
                                                1_200,
                                                300,
                                                1_500));

                AgentPerformanceSnapshot snapshot = metrics.snapshot();

                assertEquals(
                                2,
                                snapshot.llmCalls());

                assertEquals(
                                300,
                                snapshot.llmDurationMs());

                assertEquals(
                                2_000,
                                snapshot.promptTokens());

                assertEquals(
                                500,
                                snapshot.completionTokens());

                assertEquals(
                                2_500,
                                snapshot.totalTokens());

                assertEquals(
                                0,
                                snapshot.usageMissingCalls());

                assertEquals(
                                1.0,
                                snapshot.tokenUsageCoverageRatio(),
                                0.001);

                assertTrue(
                                snapshot.hasCompleteTokenUsage());
        }

        @Test
        void shouldCountMissingUsageWithoutAddingTokens() {

                AgentPerformanceMetrics metrics = new AgentPerformanceMetrics();

                metrics.recordLlmCall(
                                100);

                metrics.recordTokenUsage(
                                AgentTokenUsage.unavailable());

                metrics.recordLlmCall(
                                200);

                metrics.recordTokenUsage(
                                AgentTokenUsage.unavailable());

                AgentPerformanceSnapshot snapshot = metrics.snapshot();

                assertEquals(
                                2,
                                snapshot.llmCalls());

                assertEquals(
                                300,
                                snapshot.llmDurationMs());

                assertEquals(
                                0,
                                snapshot.promptTokens());

                assertEquals(
                                0,
                                snapshot.completionTokens());

                assertEquals(
                                0,
                                snapshot.totalTokens());

                assertEquals(
                                2,
                                snapshot.usageMissingCalls());

                assertEquals(
                                0.0,
                                snapshot.tokenUsageCoverageRatio(),
                                0.001);

                assertFalse(
                                snapshot.hasCompleteTokenUsage());
        }

        @Test
        void shouldReturnZeroRatiosWhenNoDataExists() {

                AgentPerformanceSnapshot snapshot = new AgentPerformanceMetrics()
                                .snapshot();

                assertEquals(
                                0,
                                snapshot.llmCalls());

                assertEquals(
                                0,
                                snapshot.promptTokens());

                assertEquals(
                                0,
                                snapshot.completionTokens());

                assertEquals(
                                0,
                                snapshot.totalTokens());

                assertEquals(
                                0,
                                snapshot.usageMissingCalls());

                assertEquals(
                                0.0,
                                snapshot.llmDurationRatio(),
                                0.001);

                assertEquals(
                                0.0,
                                snapshot.cacheHitRatio(),
                                0.001);

                assertEquals(
                                0.0,
                                snapshot.evidenceReductionRatio(),
                                0.001);

                assertEquals(
                                0.0,
                                snapshot.tokenUsageCoverageRatio(),
                                0.001);

                assertFalse(
                                snapshot.hasCompleteTokenUsage());
        }

        @Test
        void shouldIgnoreNegativeDurationsAndSizes() {

                AgentPerformanceMetrics metrics = new AgentPerformanceMetrics();

                metrics.recordLlmCall(
                                -100);

                metrics.recordToolCall(
                                -50);

                metrics.recordMcpCall(
                                -30);

                metrics.recordEvidenceCompaction(
                                -1_000,
                                -500);

                AgentPerformanceSnapshot snapshot = metrics.snapshot();

                assertEquals(
                                1,
                                snapshot.llmCalls());

                assertEquals(
                                0,
                                snapshot.llmDurationMs());

                assertEquals(
                                1,
                                snapshot.toolCalls());

                assertEquals(
                                0,
                                snapshot.toolDurationMs());

                assertEquals(
                                1,
                                snapshot.mcpCalls());

                assertEquals(
                                0,
                                snapshot.mcpDurationMs());

                assertEquals(
                                0,
                                snapshot.evidenceOriginalChars());

                assertEquals(
                                0,
                                snapshot.evidenceCompactedChars());
        }

        @Test
        void shouldNotInferMissingUsageFromLlmCallCount() {

                AgentPerformanceMetrics metrics = new AgentPerformanceMetrics();

                /*
                 * recordLlmCall and recordTokenUsage are intentionally
                 * separate operations.
                 *
                 * A caller that records only an LLM duration has not yet
                 * explicitly reported whether provider usage was available.
                 */
                metrics.recordLlmCall(
                                100);

                AgentPerformanceSnapshot snapshot = metrics.snapshot();

                assertEquals(
                                1,
                                snapshot.llmCalls());

                assertEquals(
                                0,
                                snapshot.usageMissingCalls());

                assertEquals(
                                1.0,
                                snapshot.tokenUsageCoverageRatio(),
                                0.001);

                assertTrue(
                                snapshot.hasCompleteTokenUsage());
        }
}