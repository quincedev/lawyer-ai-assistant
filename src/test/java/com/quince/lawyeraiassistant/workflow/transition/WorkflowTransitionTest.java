package com.quince.lawyeraiassistant.workflow.transition;

import com.quince.lawyeraiassistant.workflow.model.WorkflowContext;
import com.quince.lawyeraiassistant.workflow.model.WorkflowStatus;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkflowTransitionTest {

    @Test
    void shouldCreateUnconditionalTransition() {

        WorkflowTransition transition = WorkflowTransition.of(
                "node-a",
                "node-b");

        assertEquals(
                "node-a",
                transition.getFromNodeId());

        assertEquals(
                "node-b",
                transition.getToNodeId());
    }

    @Test
    void shouldMatchUnconditionalTransition() {

        WorkflowTransition transition = WorkflowTransition.of(
                "node-a",
                "node-b");

        WorkflowContext context = createContext(
                Map.of());

        assertTrue(
                transition.matches(
                        context));
    }

    @Test
    void shouldMatchConditionalTransition() {

        WorkflowTransition transition = WorkflowTransition.when(
                "analyze-risk",
                "human-review",
                context -> "HIGH".equals(
                        context.getVariable(
                                "riskLevel")));

        WorkflowContext context = createContext(
                Map.of(
                        "riskLevel",
                        "HIGH"));

        assertTrue(
                transition.matches(
                        context));
    }

    @Test
    void shouldNotMatchConditionalTransition() {

        WorkflowTransition transition = WorkflowTransition.when(
                "analyze-risk",
                "human-review",
                context -> "HIGH".equals(
                        context.getVariable(
                                "riskLevel")));

        WorkflowContext context = createContext(
                Map.of(
                        "riskLevel",
                        "LOW"));

        assertFalse(
                transition.matches(
                        context));
    }

    @Test
    void shouldRejectNullCondition() {

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> WorkflowTransition.when(
                        "node-a",
                        "node-b",
                        null));

        assertEquals(
                "WorkflowCondition must not be null",
                exception.getMessage());
    }

    @Test
    void shouldRejectBlankFromNodeId() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> WorkflowTransition.of(
                        "   ",
                        "node-b"));

        assertEquals(
                "From node id must not be blank",
                exception.getMessage());
    }

    @Test
    void shouldRejectBlankToNodeId() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> WorkflowTransition.of(
                        "node-a",
                        "   "));

        assertEquals(
                "To node id must not be blank",
                exception.getMessage());
    }

    private WorkflowContext createContext(
            Map<String, Object> variables) {

        return WorkflowContext.builder()
                .workflowId(
                        "workflow")
                .currentNodeId(
                        "analyze-risk")
                .status(
                        WorkflowStatus.RUNNING)
                .variables(
                        variables)
                .nodeStatuses(
                        Map.of())
                .build();
    }
}