package com.quince.lawyeraiassistant.agent.runtime.action;

import java.util.Objects;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.quince.lawyeraiassistant.agent.action.AgentActionSelector;
import com.quince.lawyeraiassistant.agent.action.policy.DuplicateToolCallPolicy;
import com.quince.lawyeraiassistant.agent.action.routing.DeterministicActionRouter;
import com.quince.lawyeraiassistant.agent.model.AgentAction;
import com.quince.lawyeraiassistant.agent.model.AgentActionExecutionResult;
import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentTask;
import com.quince.lawyeraiassistant.agent.operator.AgentActionExecutionOperator;
import com.quince.lawyeraiassistant.performance.PerformanceTimer;

@Component
public final class DefaultAgentActionCoordinator
        implements AgentActionCoordinator {

    private static final Logger log = LoggerFactory.getLogger(
            DefaultAgentActionCoordinator.class);

    private final AgentActionSelector actionSelector;

    private final AgentActionExecutionOperator actionExecutionOperator;

    private final DeterministicActionRouter deterministicActionRouter;

    private final DuplicateToolCallPolicy duplicateToolCallPolicy;

    public DefaultAgentActionCoordinator(
            AgentActionSelector actionSelector,
            AgentActionExecutionOperator actionExecutionOperator,
            DeterministicActionRouter deterministicActionRouter,
            DuplicateToolCallPolicy duplicateToolCallPolicy) {

        this.actionSelector = Objects.requireNonNull(
                actionSelector,
                "actionSelector must not be null");

        this.actionExecutionOperator = Objects.requireNonNull(
                actionExecutionOperator,
                "actionExecutionOperator must not be null");

        this.deterministicActionRouter = Objects.requireNonNull(
                deterministicActionRouter,
                "deterministicActionRouter must not be null");

        this.duplicateToolCallPolicy = Objects.requireNonNull(
                duplicateToolCallPolicy,
                "duplicateToolCallPolicy must not be null");
    }

    @Override
    public AgentAction selectAction(
            AgentContext context,
            AgentTask task) {

        Objects.requireNonNull(
                context,
                "AgentContext must not be null");

        Objects.requireNonNull(
                task,
                "AgentTask must not be null");

        AgentAction action = resolveAction(
                context,
                task);

        Objects.requireNonNull(
                action,
                "AgentAction must not be null");

        return applyDuplicateToolCallGuard(
                context,
                task,
                action);
    }

    @Override
    public AgentActionExecutionResult executeAction(
            AgentContext context,
            AgentTask task,
            AgentAction action) {

        Objects.requireNonNull(
                context,
                "AgentContext must not be null");

        Objects.requireNonNull(
                task,
                "AgentTask must not be null");

        Objects.requireNonNull(
                action,
                "AgentAction must not be null");

        AgentActionExecutionResult result = actionExecutionOperator.execute(
                context,
                task,
                action);

        return Objects.requireNonNull(
                result,
                "AgentActionExecutionResult must not be null");
    }

    private AgentAction resolveAction(
            AgentContext context,
            AgentTask task) {

        return deterministicActionRouter
                .route(
                        context,
                        task)
                .map(
                        action -> {

                            log.info(
                                    "Deterministic action selected. taskId={}, actionType={}",
                                    task.getId(),
                                    action.getType());

                            return action;
                        })
                .orElseGet(
                        () -> measureStage(
                                "ACTION_SELECTION",
                                () -> actionSelector.select(
                                        context,
                                        task)));
    }

    private AgentAction applyDuplicateToolCallGuard(
            AgentContext context,
            AgentTask currentTask,
            AgentAction action) {

        if (!action.isTool()) {
            return action;
        }

        String toolName = action.requireToolAction()
                .getToolName();

        if (!duplicateToolCallPolicy.shouldBlock(
                context,
                currentTask,
                toolName)) {

            return action;
        }

        log.warn(
                "Duplicate tool call blocked. taskId={}, toolName={}",
                currentTask.getId(),
                toolName);

        return AgentAction.reason(
                currentTask.getId());
    }

    private <T> T measureStage(
            String stage,
            Supplier<T> operation) {

        Objects.requireNonNull(
                stage,
                "stage must not be null");

        Objects.requireNonNull(
                operation,
                "operation must not be null");

        PerformanceTimer timer = PerformanceTimer.start();

        try {

            return operation.get();

        } finally {

            log.info(
                    "Agent stage finished. stage={}, durationMs={}",
                    stage,
                    timer.elapsedMillis());
        }
    }
}