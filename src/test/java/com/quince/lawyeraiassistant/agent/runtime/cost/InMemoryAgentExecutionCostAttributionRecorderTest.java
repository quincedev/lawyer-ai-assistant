package com.quince.lawyeraiassistant.agent.runtime.cost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.quince.lawyeraiassistant.agent.model.AgentStatus;
import com.quince.lawyeraiassistant.agent.runtime.observability.AgentExecutionStatus;

class InMemoryAgentExecutionCostAttributionRecorderTest {

    private final InMemoryAgentExecutionCostAttributionRecorder recorder =
            new InMemoryAgentExecutionCostAttributionRecorder();

    @Test
    void shouldInitiallyHaveNoLatestAttribution() {

        assertTrue(recorder.latest().isEmpty());
    }

    @Test
    void shouldReturnLatestAttributionAfterRecord() {

        AgentExecutionCostAttribution attribution = attribution("execution-001");

        recorder.record(attribution);

        assertEquals(attribution, recorder.latest().orElseThrow());
    }

    @Test
    void shouldReplaceOldAttributionWithNewRecord() {

        recorder.record(attribution("execution-001"));
        AgentExecutionCostAttribution latest = attribution("execution-002");
        recorder.record(latest);

        assertEquals(latest, recorder.latest().orElseThrow());
    }

    @Test
    void shouldClearLatestAttribution() {

        recorder.record(attribution("execution-001"));

        recorder.clear();

        assertTrue(recorder.latest().isEmpty());
    }

    private AgentExecutionCostAttribution attribution(
            String executionId) {

        return new AgentExecutionCostAttribution(
                executionId,
                "tenant-test-001",
                AgentExecutionStatus.SUCCESS,
                AgentStatus.FINISHED,
                Instant.parse("2026-08-27T00:00:00Z"),
                100,
                1,
                10,
                5,
                15,
                0,
                1,
                0,
                "USD",
                25);
    }
}
