package com.quince.lawyeraiassistant.agent.action.policy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentTask;
import com.quince.lawyeraiassistant.agent.model.ToolObservation;

class NoProgressRetryPolicyTest {

    private final NoProgressRetryPolicy policy = new NoProgressRetryPolicy();

    @Test
    void shouldNotBlockWhenThereIsOnlyOneSuccessfulObservation() {

        AgentTask task = AgentTask.pending(
                "task-1",
                "检索法律依据");

        AgentContext context = AgentContext.from(
                "分析违法解除")
                .appendObservation(
                        ToolObservation.success(
                                "task-1",
                                "searchLegalKnowledge",
                                "evidence-A"));

        assertFalse(
                policy.isNoProgress(
                        context,
                        task));
    }

    @Test
    void shouldDetectIdenticalEvidenceForSameTaskAndTool() {

        AgentTask task = AgentTask.pending(
                "task-1",
                "检索法律依据");

        AgentContext context = AgentContext.from(
                "分析违法解除")
                .appendObservation(
                        ToolObservation.success(
                                "task-1",
                                "searchLegalKnowledge",
                                "evidence-A"))
                .appendObservation(
                        ToolObservation.success(
                                "task-1",
                                "searchLegalKnowledge",
                                "evidence-A"));

        assertTrue(
                policy.isNoProgress(
                        context,
                        task));
    }

    @Test
    void shouldAllowRetryWhenEvidenceChanged() {

        AgentTask task = AgentTask.pending(
                "task-1",
                "检索法律依据");

        AgentContext context = AgentContext.from(
                "分析违法解除")
                .appendObservation(
                        ToolObservation.success(
                                "task-1",
                                "searchLegalKnowledge",
                                "evidence-A"))
                .appendObservation(
                        ToolObservation.success(
                                "task-1",
                                "searchLegalKnowledge",
                                "evidence-B"));

        assertFalse(
                policy.isNoProgress(
                        context,
                        task));
    }

    @Test
    void shouldIgnoreObservationFromDifferentTask() {

        AgentTask task = AgentTask.pending(
                "task-2",
                "检索法律依据");

        AgentContext context = AgentContext.from(
                "分析违法解除")
                .appendObservation(
                        ToolObservation.success(
                                "task-1",
                                "searchLegalKnowledge",
                                "same-evidence"))
                .appendObservation(
                        ToolObservation.success(
                                "task-2",
                                "searchLegalKnowledge",
                                "same-evidence"));

        assertFalse(
                policy.isNoProgress(
                        context,
                        task));
    }

    @Test
    void shouldIgnoreDifferentTool() {

        AgentTask task = AgentTask.pending(
                "task-1",
                "检索法律依据");

        AgentContext context = AgentContext.from(
                "分析违法解除")
                .appendObservation(
                        ToolObservation.success(
                                "task-1",
                                "tool-a",
                                "same-evidence"))
                .appendObservation(
                        ToolObservation.success(
                                "task-1",
                                "tool-b",
                                "same-evidence"));

        assertFalse(
                policy.isNoProgress(
                        context,
                        task));
    }

    @Test
    void shouldIgnoreFailedObservation() {

        AgentTask task = AgentTask.pending(
                "task-1",
                "检索法律依据");

        AgentContext context = AgentContext.from(
                "分析违法解除")
                .appendObservation(
                        ToolObservation.success(
                                "task-1",
                                "searchLegalKnowledge",
                                "same-evidence"))
                .appendObservation(
                        ToolObservation.failure(
                                "task-1",
                                "searchLegalKnowledge",
                                "tool failed"));

        assertFalse(
                policy.isNoProgress(
                        context,
                        task));
    }
}