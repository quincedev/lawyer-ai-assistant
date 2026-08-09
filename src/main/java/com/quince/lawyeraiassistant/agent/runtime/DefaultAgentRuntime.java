package com.quince.lawyeraiassistant.agent.runtime;

import com.quince.lawyeraiassistant.agent.action.AgentActionSelector;
import com.quince.lawyeraiassistant.agent.model.AgentAction;
import com.quince.lawyeraiassistant.agent.model.AgentActionExecutionResult;
import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentPlan;
import com.quince.lawyeraiassistant.agent.model.AgentStatus;
import com.quince.lawyeraiassistant.agent.model.AgentTask;
import com.quince.lawyeraiassistant.agent.model.AgentTaskStatus;
import com.quince.lawyeraiassistant.agent.model.ReflectionResult;
import com.quince.lawyeraiassistant.agent.model.RuntimeReasonObservation;
import com.quince.lawyeraiassistant.agent.model.ToolObservation;
import com.quince.lawyeraiassistant.agent.operator.AgentActionExecutionOperator;
import com.quince.lawyeraiassistant.agent.pipeline.AgentPipeline;
import com.quince.lawyeraiassistant.agent.service.AgentFinalAnswerService;
import com.quince.lawyeraiassistant.agent.service.AgentReflectionService;
import com.quince.lawyeraiassistant.agent.service.AgentReplanningService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class DefaultAgentRuntime
                implements AgentRuntime {

        private static final String AGENT_FINISHED_LOG = "Agent finished";

        private final AgentPipeline agentPipeline;

        private final AgentActionSelector actionSelector;

        private final AgentActionExecutionOperator actionExecutionOperator;

        private final AgentReflectionService reflectionService;

        private final AgentReplanningService replanningService;

        private final AgentFinalAnswerService finalAnswerService;

        private final int maxSteps;

        private final int maxRetriesPerTask;

        private final int maxReplans;

        public DefaultAgentRuntime(
                        AgentPipeline agentPipeline,
                        AgentActionSelector actionSelector,
                        AgentActionExecutionOperator actionExecutionOperator,
                        AgentReflectionService reflectionService,
                        AgentReplanningService replanningService,
                        AgentFinalAnswerService finalAnswerService,
                        @Value("${agent.runtime.max-steps:10}") int maxSteps,
                        @Value("${agent.runtime.max-retries-per-task:2}") int maxRetriesPerTask,
                        @Value("${agent.runtime.max-replans:2}") int maxReplans) {

                this.agentPipeline = Objects.requireNonNull(
                                agentPipeline,
                                "agentPipeline must not be null");

                this.actionSelector = Objects.requireNonNull(
                                actionSelector,
                                "actionSelector must not be null");

                this.actionExecutionOperator = Objects.requireNonNull(
                                actionExecutionOperator,
                                "actionExecutionOperator must not be null");

                this.reflectionService = Objects.requireNonNull(
                                reflectionService,
                                "reflectionService must not be null");

                this.replanningService = Objects.requireNonNull(
                                replanningService,
                                "replanningService must not be null");

                this.finalAnswerService = Objects.requireNonNull(
                                finalAnswerService,
                                "finalAnswerService must not be null");

                if (maxSteps <= 0) {
                        throw new IllegalArgumentException(
                                        "maxSteps must be greater than zero");
                }

                if (maxRetriesPerTask < 0) {
                        throw new IllegalArgumentException(
                                        "maxRetriesPerTask must not be negative");
                }

                if (maxReplans < 0) {
                        throw new IllegalArgumentException(
                                        "maxReplans must not be negative");
                }

                this.maxSteps = maxSteps;

                this.maxRetriesPerTask = maxRetriesPerTask;

                this.maxReplans = maxReplans;
        }

        @Override
        public AgentContext run(
                        AgentContext context) {

                Objects.requireNonNull(
                                context,
                                "AgentContext must not be null");

                /*
                 * Initialization:
                 *
                 * Reason
                 * ↓
                 * Planning
                 */
                AgentContext current = agentPipeline.execute(
                                context);

                int executedSteps = 0;

                int replanCount = 0;

                Map<String, Integer> retryCounts = new HashMap<>();

                /*
                 * Full Agent Loop:
                 *
                 * Current Task
                 * ↓
                 * Action Selection
                 * ↓
                 * Action Execution
                 * ↓
                 * Apply Result
                 * ↓
                 * Reflection
                 * ↓
                 * CONTINUE / RETRY / REPLAN / FINISH
                 */
                while (hasPendingTask(
                                current)
                                && executedSteps < maxSteps) {

                        /*
                         * 1. Resolve current task.
                         */
                        AgentTask pendingTask = current.getAgentPlan()
                                        .nextPendingTask()
                                        .orElseThrow();

                        /*
                         * 2. PENDING → RUNNING
                         */
                        current = updateTaskStatus(
                                        current,
                                        pendingTask.getId(),
                                        AgentTaskStatus.RUNNING);

                        AgentTask runningTask = current.getAgentPlan()
                                        .findTaskById(
                                                        pendingTask.getId())
                                        .orElseThrow();

                        /*
                         * 3. Decide.
                         */
                        AgentAction action = actionSelector.select(
                                        current,
                                        runningTask);

                        /*
                         * 4. Act.
                         */
                        AgentActionExecutionResult result = actionExecutionOperator.execute(
                                        current,
                                        runningTask,
                                        action);

                        /*
                         * 5. Observe / Apply.
                         */
                        current = applyExecutionResult(
                                        current,
                                        runningTask,
                                        result);

                        executedSteps++;

                        /*
                         * FINAL_ANSWER is terminal.
                         *
                         * Final Answer 已经生成，
                         * 不需要再次 Reflection。
                         */
                        if (current.hasFinalAnswer()) {

                                return markFinished(
                                                current);
                        }

                        /*
                         * 6. Reflect.
                         */
                        ReflectionResult reflectionResult = reflectionService.reflect(
                                        current,
                                        runningTask);

                        /*
                         * =====================================================
                         * RETRY Guardrail
                         * =====================================================
                         */
                        if (reflectionResult.shouldRetry()) {

                                int retryCount = retryCounts.getOrDefault(
                                                runningTask.getId(),
                                                0);

                                /*
                                 * maxRetriesPerTask = 2 时：
                                 *
                                 * 初始执行
                                 * RETRY #1
                                 * RETRY #2
                                 *
                                 * 第三次再次要求 RETRY 时，
                                 * Guardrail 生效。
                                 */
                                if (retryCount >= maxRetriesPerTask) {

                                        return finishWithFallback(
                                                        current,
                                                        "Maximum retries reached for task: "
                                                                        + runningTask.getId());
                                }

                                retryCounts.put(
                                                runningTask.getId(),
                                                retryCount + 1);
                        }

                        /*
                         * =====================================================
                         * REPLAN Guardrail
                         * =====================================================
                         */
                        if (reflectionResult.shouldReplan()) {

                                if (replanCount >= maxReplans) {

                                        return finishWithFallback(
                                                        current,
                                                        "Maximum replans reached");
                                }

                                replanCount++;
                        }

                        /*
                         * 7. Apply Reflection Decision.
                         */
                        current = handleReflectionDecision(
                                        current,
                                        runningTask,
                                        reflectionResult);

                        /*
                         * 当前任务已经正常完成，
                         * 不再需要保存它的 Retry 计数。
                         */
                        if (reflectionResult.shouldContinue()
                                        || reflectionResult.shouldFinish()) {

                                retryCounts.remove(
                                                runningTask.getId());
                        }

                        /*
                         * Replanning 得到的是新的 Plan。
                         *
                         * 新 Plan 的 Task 即使 ID 与旧 Plan 相同，
                         * 也应重新获得完整 Retry Budget。
                         */
                        if (reflectionResult.shouldReplan()) {

                                retryCounts.clear();
                        }

                        /*
                         * FINISH decision is terminal.
                         */
                        if (current.getStatus() == AgentStatus.FINISHED) {

                                return current;
                        }
                }

                /*
                 * 所有 Pending Task 已经执行完成，
                 * 但没有显式产生 FINAL_ANSWER。
                 */
                if (!hasPendingTask(
                                current)) {

                        current = ensureFinalAnswer(
                                        current);

                        return markFinished(
                                        current);
                }

                /*
                 * maxSteps exhausted.
                 *
                 * 不再返回 RUNNING，
                 * 而是根据已有上下文生成 best-effort answer，
                 * 然后安全结束。
                 */
                return finishWithFallback(
                                current,
                                "Maximum execution steps reached: "
                                                + maxSteps);
        }

        /*
         * =========================================================
         * Action Execution Result
         * =========================================================
         */

        private AgentContext applyExecutionResult(
                        AgentContext context,
                        AgentTask task,
                        AgentActionExecutionResult result) {

                Objects.requireNonNull(
                                result,
                                "AgentActionExecutionResult must not be null");

                return switch (result.getActionType()) {

                        case TOOL ->
                                applyToolResult(
                                                context,
                                                task,
                                                result.getObservation());

                        case REASON ->
                                applyReasonResult(
                                                context,
                                                task,
                                                result.getContent());

                        case FINAL_ANSWER ->
                                applyFinalAnswerResult(
                                                context,
                                                task,
                                                result.getContent());
                };
        }

        private AgentContext applyToolResult(
                        AgentContext context,
                        AgentTask task,
                        ToolObservation observation) {

                Objects.requireNonNull(
                                observation,
                                "ToolObservation must not be null");

                AgentTaskStatus newStatus = observation.isSuccess()
                                ? AgentTaskStatus.COMPLETED
                                : AgentTaskStatus.FAILED;

                AgentContext updated = updateTaskStatus(
                                context,
                                task.getId(),
                                newStatus)
                                .appendObservation(
                                                observation);

                return updated.appendExecutionLog(
                                observation.isSuccess()
                                                ? "Tool action completed: "
                                                                + task.getId()
                                                : "Tool action failed: "
                                                                + task.getId());
        }

        private AgentContext applyReasonResult(
                        AgentContext context,
                        AgentTask task,
                        String content) {

                RuntimeReasonObservation observation = RuntimeReasonObservation.of(
                                task.getId(),
                                content);

                return updateTaskStatus(
                                context,
                                task.getId(),
                                AgentTaskStatus.COMPLETED)
                                .appendRuntimeReasonObservation(
                                                observation)
                                .appendExecutionLog(
                                                "Reason action completed: "
                                                                + task.getId());
        }

        private AgentContext applyFinalAnswerResult(
                        AgentContext context,
                        AgentTask task,
                        String content) {

                return updateTaskStatus(
                                context,
                                task.getId(),
                                AgentTaskStatus.COMPLETED)
                                .withFinalAnswer(
                                                content)
                                .appendExecutionLog(
                                                "Final answer generated: "
                                                                + task.getId());
        }

        /*
         * =========================================================
         * Reflection Decision
         * =========================================================
         */

        private AgentContext handleReflectionDecision(
                        AgentContext context,
                        AgentTask task,
                        ReflectionResult reflectionResult) {

                Objects.requireNonNull(
                                reflectionResult,
                                "ReflectionResult must not be null");

                return switch (reflectionResult.getDecision()) {

                        case CONTINUE ->
                                handleContinue(
                                                context,
                                                task,
                                                reflectionResult);

                        case RETRY ->
                                handleRetry(
                                                context,
                                                task,
                                                reflectionResult);

                        case REPLAN ->
                                handleReplan(
                                                context,
                                                task,
                                                reflectionResult);

                        case FINISH ->
                                handleFinish(
                                                context,
                                                reflectionResult);
                };
        }

        private AgentContext handleContinue(
                        AgentContext context,
                        AgentTask task,
                        ReflectionResult reflectionResult) {

                return context.appendExecutionLog(
                                "Reflection CONTINUE: "
                                                + task.getId()
                                                + " - "
                                                + reflectionResult.getSummary());
        }

        private AgentContext handleRetry(
                        AgentContext context,
                        AgentTask task,
                        ReflectionResult reflectionResult) {

                AgentContext updated = updateTaskStatus(
                                context,
                                task.getId(),
                                AgentTaskStatus.PENDING);

                return updated.appendExecutionLog(
                                "Reflection RETRY: "
                                                + task.getId()
                                                + " - "
                                                + reflectionResult.getSummary());
        }

        private AgentContext handleReplan(
                        AgentContext context,
                        AgentTask task,
                        ReflectionResult reflectionResult) {

                AgentPlan replanned = replanningService.replan(
                                context,
                                reflectionResult);

                Objects.requireNonNull(
                                replanned,
                                "Replanned AgentPlan must not be null");

                return context.withAgentPlan(
                                replanned)
                                .appendExecutionLog(
                                                "Reflection REPLAN: "
                                                                + task.getId()
                                                                + " - "
                                                                + reflectionResult.getSummary());
        }

        private AgentContext handleFinish(
                        AgentContext context,
                        ReflectionResult reflectionResult) {

                AgentContext updated = ensureFinalAnswer(
                                context)
                                .appendExecutionLog(
                                                "Reflection FINISH: "
                                                                + reflectionResult.getSummary());

                return markFinished(
                                updated);
        }

        /*
         * =========================================================
         * Runtime Guardrail
         * =========================================================
         */

        private AgentContext finishWithFallback(
                        AgentContext context,
                        String reason) {

                AgentContext updated = context.appendExecutionLog(
                                "Runtime guardrail triggered: "
                                                + reason);

                updated = ensureFinalAnswer(
                                updated);

                return markFinished(
                                updated);
        }

        /*
         * =========================================================
         * Runtime Helpers
         * =========================================================
         */

        private AgentContext ensureFinalAnswer(
                        AgentContext context) {

                if (context.hasFinalAnswer()) {
                        return context;
                }

                String finalAnswer = finalAnswerService.generate(
                                context);

                return context.withFinalAnswer(
                                finalAnswer)
                                .appendExecutionLog(
                                                "Final answer generated");
        }

        private AgentContext updateTaskStatus(
                        AgentContext context,
                        String taskId,
                        AgentTaskStatus status) {

                AgentPlan updatedPlan = context.getAgentPlan()
                                .updateTaskStatus(
                                                taskId,
                                                status);

                return context.withAgentPlan(
                                updatedPlan);
        }

        private boolean hasPendingTask(
                        AgentContext context) {

                return context.getAgentPlan()
                                .nextPendingTask()
                                .isPresent();
        }

        private AgentContext markFinished(
                        AgentContext context) {

                if (context.getStatus() == AgentStatus.FINISHED) {

                        return context;
                }

                return context.toBuilder()
                                .status(
                                                AgentStatus.FINISHED)
                                .build()
                                .appendExecutionLog(
                                                AGENT_FINISHED_LOG);
        }
}