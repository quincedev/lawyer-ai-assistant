package com.quince.lawyeraiassistant.agent.runtime.action;

import com.quince.lawyeraiassistant.agent.action.AgentActionSelector;
import com.quince.lawyeraiassistant.agent.action.policy.DuplicateToolCallPolicy;
import com.quince.lawyeraiassistant.agent.action.routing.DeterministicActionRouter;
import com.quince.lawyeraiassistant.agent.model.AgentAction;
import com.quince.lawyeraiassistant.agent.model.AgentActionExecutionResult;
import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentTask;
import com.quince.lawyeraiassistant.agent.model.ToolAction;
import com.quince.lawyeraiassistant.agent.operator.AgentActionExecutionOperator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DefaultAgentActionCoordinatorTest {

    private AgentActionSelector actionSelector;

    private AgentActionExecutionOperator actionExecutionOperator;

    private DeterministicActionRouter deterministicActionRouter;

    private DuplicateToolCallPolicy duplicateToolCallPolicy;

    private DefaultAgentActionCoordinator coordinator;

    private AgentContext context;

    private AgentTask task;

    @BeforeEach
    void setUp() {

        actionSelector = mock(AgentActionSelector.class);
        actionExecutionOperator = mock(AgentActionExecutionOperator.class);
        deterministicActionRouter = mock(DeterministicActionRouter.class);
        duplicateToolCallPolicy = mock(DuplicateToolCallPolicy.class);

        coordinator = new DefaultAgentActionCoordinator(
                actionSelector,
                actionExecutionOperator,
                deterministicActionRouter,
                duplicateToolCallPolicy);

        context = AgentContext.from("Resolve the legal issue");
        task = AgentTask.pending("task-1", "Review the evidence");
    }

    @Test
    void shouldReturnDeterministicActionWithoutCallingSelectorWhenRouteHits() {

        AgentAction deterministicAction = AgentAction.reason(task.getId());

        when(deterministicActionRouter.route(context, task))
                .thenReturn(Optional.of(deterministicAction));

        AgentAction result = coordinator.selectAction(context, task);

        assertSame(deterministicAction, result);
        verify(actionSelector, never()).select(context, task);
    }

    @Test
    void shouldReturnSelectorActionWhenDeterministicRouteMisses() {

        AgentAction selectorAction = AgentAction.finalAnswer(task.getId());

        when(deterministicActionRouter.route(context, task))
                .thenReturn(Optional.empty());
        when(actionSelector.select(context, task))
                .thenReturn(selectorAction);

        AgentAction result = coordinator.selectAction(context, task);

        assertSame(selectorAction, result);
        verify(actionSelector).select(context, task);
    }

    @Test
    void shouldReturnNonToolActionWithoutCheckingDuplicatePolicy() {

        AgentAction action = AgentAction.reason(task.getId());

        when(deterministicActionRouter.route(context, task))
                .thenReturn(Optional.of(action));

        AgentAction result = coordinator.selectAction(context, task);

        assertSame(action, result);
        verifyNoInteractions(duplicateToolCallPolicy);
    }

    @Test
    void shouldReturnOriginalToolActionWhenItIsNotDuplicate() {

        AgentAction action = toolAction();

        when(deterministicActionRouter.route(context, task))
                .thenReturn(Optional.of(action));
        when(duplicateToolCallPolicy.shouldBlock(
                context,
                task,
                "searchLegalKnowledge"))
                .thenReturn(false);

        AgentAction result = coordinator.selectAction(context, task);

        assertSame(action, result);
        verify(duplicateToolCallPolicy).shouldBlock(
                context,
                task,
                "searchLegalKnowledge");
    }

    @Test
    void shouldReturnReasonActionWithSameTaskIdWhenToolCallIsDuplicate() {

        AgentAction action = toolAction();

        when(deterministicActionRouter.route(context, task))
                .thenReturn(Optional.of(action));
        when(duplicateToolCallPolicy.shouldBlock(
                context,
                task,
                "searchLegalKnowledge"))
                .thenReturn(true);

        AgentAction result = coordinator.selectAction(context, task);

        assertTrue(result.isReason());
        assertEquals(task.getId(), result.getTaskId());
    }

    @Test
    void shouldDelegateActionExecutionOnceAndReturnSameResult() {

        AgentAction action = AgentAction.reason(task.getId());
        AgentActionExecutionResult executionResult =
                AgentActionExecutionResult.reason("Reasoned response");

        when(actionExecutionOperator.execute(context, task, action))
                .thenReturn(executionResult);

        AgentActionExecutionResult result = coordinator.executeAction(
                context,
                task,
                action);

        assertSame(executionResult, result);
        verify(actionExecutionOperator).execute(context, task, action);
    }

    private AgentAction toolAction() {

        return AgentAction.tool(
                ToolAction.of(
                        task.getId(),
                        "searchLegalKnowledge"));
    }
}
