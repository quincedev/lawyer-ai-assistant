package com.quince.lawyeraiassistant.agent.runtime.cost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.quince.lawyeraiassistant.agent.model.AgentStatus;
import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceSnapshot;
import com.quince.lawyeraiassistant.agent.runtime.observability.AgentExecutionStatus;

class AgentExecutionCostAttributionTest {

    @Test
    void shouldCreateAttributionFromSnapshot() {

        AgentExecutionCostAttribution attribution = attribution(
                snapshot(4, 0, 4, 0, 1_250_000));

        assertEquals("execution-test-001", attribution.executionId());
        assertEquals("tenant-test-001", attribution.tenantId());
        assertEquals(AgentExecutionStatus.SUCCESS, attribution.executionStatus());
        assertEquals(AgentStatus.FINISHED, attribution.agentStatus());
        assertEquals(12_000, attribution.totalDurationMs());
        assertEquals(4, attribution.llmCalls());
        assertEquals(1_000, attribution.promptTokens());
        assertEquals(250, attribution.completionTokens());
        assertEquals(1_250, attribution.totalTokens());
        assertEquals("USD", attribution.currency());
        assertEquals(1_250_000, attribution.costMicros());
    }

    @Test
    void shouldConvertMicroUsdToUsd() {

        assertEquals(
                new BigDecimal("1.25"),
                attribution(snapshot(4, 0, 4, 0, 1_250_000)).cost());
    }

    @Test
    void shouldCalculateTokenCoverage() {

        assertEquals(
                0.75D,
                attribution(snapshot(4, 1, 3, 0, 10)).tokenUsageCoverageRatio(),
                0.0001D);
    }

    @Test
    void shouldCalculatePricingCoverage() {

        assertEquals(
                0.75D,
                attribution(snapshot(4, 0, 3, 1, 10)).pricingCoverageRatio(),
                0.0001D);
    }

    @Test
    void shouldBeFullyAttributedWhenUsageAndPricingAreComplete() {

        assertTrue(
                attribution(snapshot(4, 0, 4, 0, 10)).fullyAttributed());
    }

    @Test
    void shouldNotBeFullyAttributedWhenPricingIsMissing() {

        assertFalse(
                attribution(snapshot(4, 0, 3, 1, 10)).fullyAttributed());
    }

    private AgentExecutionCostAttribution attribution(
            AgentPerformanceSnapshot snapshot) {

        return AgentExecutionCostAttribution.from(
                "execution-test-001",
                "tenant-test-001",
                AgentExecutionStatus.SUCCESS,
                AgentStatus.FINISHED,
                "usd",
                snapshot);
    }

    private AgentPerformanceSnapshot snapshot(
            int llmCalls,
            int usageMissingCalls,
            long pricedLlmCalls,
            long pricingMissingCalls,
            long costMicros) {

        return new AgentPerformanceSnapshot(
                12_000,
                llmCalls,
                10_000,
                1_000,
                250,
                1_250,
                usageMissingCalls,
                2,
                500,
                1,
                200,
                1,
                1,
                2_000,
                1_000,
                1,
                0,
                costMicros,
                pricedLlmCalls,
                pricingMissingCalls);
    }
}
