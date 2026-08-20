package com.quince.lawyeraiassistant.agent.action.policy;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentTask;
import com.quince.lawyeraiassistant.agent.model.ToolObservation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DuplicateToolCallPolicyTest {

    private static final String TOOL_NAME = "searchLegalKnowledge";

    private DuplicateToolCallPolicy policy;

    @BeforeEach
    void setUp() {

        policy = new DuplicateToolCallPolicy();
    }

    @Test
    void shouldBlockSuccessfulDuplicateToolCallForAnalyticalTask() {

        AgentContext context = AgentContext.from("Analyze legal issue")
                .appendObservation(ToolObservation.success(
                        "task-1",
                        TOOL_NAME,
                        "existing evidence"));

        boolean result = policy.shouldBlock(
                context,
                AgentTask.pending("task-2", "分析已有证据并形成结论"),
                TOOL_NAME);

        assertTrue(result);
    }

    @Test
    void shouldAllowSuccessfulDuplicateToolCallForExplicitRetrievalTask() {

        AgentContext context = AgentContext.from("Research legal issue")
                .appendObservation(ToolObservation.success(
                        "task-1",
                        TOOL_NAME,
                        "existing evidence"));

        boolean result = policy.shouldBlock(
                context,
                AgentTask.pending("task-2", "检索最新法律依据"),
                TOOL_NAME);

        assertFalse(result);
    }

    @Test
    void shouldBlockDuplicateToolCallAfterObservationLengthFailure() {

        AgentContext context = AgentContext.from("Research legal issue")
                .appendObservation(ToolObservation.failure(
                        "task-1",
                        TOOL_NAME,
                        "Maximum Observation length exceeded"));

        boolean result = policy.shouldBlock(
                context,
                AgentTask.pending("task-1", "检索法律依据"),
                TOOL_NAME);

        assertTrue(result);
    }

    @Test
    void shouldAllowDuplicateToolCallAfterTimeout() {

        AgentContext context = AgentContext.from("Research legal issue")
                .appendObservation(ToolObservation.failure(
                        "task-1",
                        TOOL_NAME,
                        "Tool execution timeout"));

        boolean result = policy.shouldBlock(
                context,
                AgentTask.pending("task-1", "检索法律依据"),
                TOOL_NAME);

        assertFalse(result);
    }
}
