package com.quince.lawyeraiassistant.agent.runtime;

import com.quince.lawyeraiassistant.agent.action.policy.NoProgressRetryPolicy;
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
import com.quince.lawyeraiassistant.agent.pipeline.AgentPipeline;
import com.quince.lawyeraiassistant.agent.runtime.action.AgentActionCoordinator;
import com.quince.lawyeraiassistant.agent.runtime.evidence.AgentEvidencePolicy;
import com.quince.lawyeraiassistant.agent.runtime.evidence.EvidenceValidationResult;
import com.quince.lawyeraiassistant.agent.runtime.observability.AgentExecutionObservation;
import com.quince.lawyeraiassistant.agent.runtime.observability.AgentExecutionObserver;
import com.quince.lawyeraiassistant.agent.runtime.observability.AgentExecutionStatus;
import com.quince.lawyeraiassistant.agent.runtime.policy.AgentExecutionPolicyFacade;
import com.quince.lawyeraiassistant.agent.runtime.resource.AgentContextSizeEstimator;
import com.quince.lawyeraiassistant.agent.service.AgentFinalAnswerService;
import com.quince.lawyeraiassistant.agent.service.AgentReflectionService;
import com.quince.lawyeraiassistant.agent.service.AgentReplanningService;
import com.quince.lawyeraiassistant.agent.skill.context.SkillContext;
import com.quince.lawyeraiassistant.agent.skill.selector.AgentSkillSelector;
import com.quince.lawyeraiassistant.agent.stream.AgentStreamEvent;
import com.quince.lawyeraiassistant.agent.stream.AgentStreamEventType;
import com.quince.lawyeraiassistant.agent.stream.AgentStreamPublisher;
import com.quince.lawyeraiassistant.agent.stream.NoOpAgentStreamPublisher;
import com.quince.lawyeraiassistant.performance.PerformanceTimer;
import com.quince.lawyeraiassistant.security.legal.LegalSecurityContext;
import com.quince.lawyeraiassistant.security.legal.SecuritySource;
import com.quince.lawyeraiassistant.security.legal.SecurityTrustLevel;
import com.quince.lawyeraiassistant.security.runtime.AgentExecutionBudget;
import com.quince.lawyeraiassistant.security.runtime.RuntimeGuardrailOperation;
import com.quince.lawyeraiassistant.security.runtime.RuntimeGuardrailResult;
import com.quince.lawyeraiassistant.security.runtime.resource.RuntimeResourceResult;
import com.quince.lawyeraiassistant.security.runtime.resource.RuntimeResourceType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class DefaultAgentRuntime
                implements AgentRuntime {

        private static final Logger log = LoggerFactory.getLogger(
                        DefaultAgentRuntime.class);

        private static final String AGENT_FINISHED_LOG = "Agent finished";

        private final AgentPipeline agentPipeline;

        private final AgentReflectionService reflectionService;

        private final AgentReplanningService replanningService;

        private final AgentFinalAnswerService finalAnswerService;

        private final AgentSkillSelector skillSelector;

        private final AgentEvidencePolicy evidencePolicy;

        private final AgentContextSizeEstimator contextSizeEstimator;

        private final NoProgressRetryPolicy noProgressRetryPolicy;

        private final AgentExecutionPolicyFacade executionPolicy;

        private final AgentActionCoordinator actionCoordinator;

        private final AgentExecutionObserver executionObserver;

        public DefaultAgentRuntime(
                        AgentPipeline agentPipeline,
                        AgentSkillSelector skillSelector,
                        AgentReflectionService reflectionService,
                        AgentReplanningService replanningService,
                        AgentFinalAnswerService finalAnswerService,
                        AgentExecutionPolicyFacade executionPolicy,
                        AgentEvidencePolicy evidencePolicy,
                        AgentContextSizeEstimator contextSizeEstimator,
                        NoProgressRetryPolicy noProgressRetryPolicy,
                        AgentActionCoordinator actionCoordinator,
                        AgentExecutionObserver executionObserver) {

                this.agentPipeline = Objects.requireNonNull(
                                agentPipeline,
                                "agentPipeline must not be null");

                this.skillSelector = Objects.requireNonNull(
                                skillSelector,
                                "skillSelector must not be null");

                this.reflectionService = Objects.requireNonNull(
                                reflectionService,
                                "reflectionService must not be null");

                this.replanningService = Objects.requireNonNull(
                                replanningService,
                                "replanningService must not be null");

                this.finalAnswerService = Objects.requireNonNull(
                                finalAnswerService,
                                "finalAnswerService must not be null");

                this.executionPolicy = Objects.requireNonNull(
                                executionPolicy,
                                "executionPolicy must not be null");

                this.evidencePolicy = Objects.requireNonNull(
                                evidencePolicy,
                                "evidencePolicy must not be null");

                this.contextSizeEstimator = Objects.requireNonNull(
                                contextSizeEstimator,
                                "contextSizeEstimator must not be null");

                this.noProgressRetryPolicy = Objects.requireNonNull(
                                noProgressRetryPolicy,
                                "noProgressRetryPolicy must not be null");

                this.actionCoordinator = Objects.requireNonNull(
                                actionCoordinator,
                                "actionCoordinator must not be null");

                this.executionObserver = Objects.requireNonNull(
                                executionObserver,
                                "executionObserver must not be null");
        }

        @Override
        public AgentContext run(
                        AgentContext context) {

                return run(
                                context,
                                NoOpAgentStreamPublisher.INSTANCE);
        }

        @Override
        public AgentContext run(
                        AgentContext context,
                        AgentStreamPublisher publisher) {

                Objects.requireNonNull(
                                context,
                                "AgentContext must not be null");

                Objects.requireNonNull(
                                publisher,
                                "AgentStreamPublisher must not be null");

                AgentContext result = null;

                AgentExecutionStatus executionStatus = AgentExecutionStatus.FAILED;

                AgentExecutionObservation observation = executionObserver.start();

                try {

                        publisher.publish(
                                        AgentStreamEvent.of(
                                                        AgentStreamEventType.AGENT_STARTED,
                                                        "Agent execution started"));

                        result = runInternal(
                                        context,
                                        publisher);

                        executionStatus = AgentExecutionStatus.SUCCESS;

                        return result;

                } catch (RuntimeException exception) {

                        publisher.publish(
                                        AgentStreamEvent.of(
                                                        AgentStreamEventType.AGENT_FAILED,
                                                        "Agent execution failed"));

                        throw exception;

                } finally {

                        executionObserver.complete(
                                        observation,
                                        context,
                                        result,
                                        executionStatus);
                }
        }

        private AgentContext runInternal(
                        AgentContext context,
                        AgentStreamPublisher publisher) {
                /*
                 * 每次 Agent Execution 独享一个 Budget。
                 *
                 * DefaultAgentRuntime 是 Singleton Bean，
                 * 所以 Budget 绝不能放到成员变量中。
                 */
                AgentExecutionBudget budget = executionPolicy.createBudget();

                AgentContext skillAwareContext = attachSelectedSkill(
                                context);

                publisher.publish(
                                AgentStreamEvent.of(
                                                AgentStreamEventType.PLANNING_STARTED,
                                                "正在生成执行计划"));

                /*
                 * =====================================================
                 * Initialization
                 *
                 * Reason
                 * ↓
                 * Planning
                 *
                 * Initial Planning 不计入 REPLAN Budget。
                 * =====================================================
                 */
                AgentContext current = agentPipeline.execute(
                                skillAwareContext);

                publisher.publish(
                                AgentStreamEvent.of(
                                                AgentStreamEventType.PLANNING_COMPLETED,
                                                "执行计划生成完成"));

                /*
                 * =====================================================
                 * Full Agent Runtime Loop
                 * =====================================================
                 *
                 * Pending Task
                 * ↓
                 * Runtime STEP Guardrail
                 * ↓
                 * PENDING → RUNNING
                 * ↓
                 * Action Selection
                 * ↓
                 * TOOL_CALL Guardrail
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
                                current)) {

                        /*
                         * =================================================
                         * 1. STEP Guardrail
                         * =================================================
                         */
                        RuntimeGuardrailResult stepDecision = executionPolicy.evaluateRuntime(
                                        RuntimeGuardrailOperation.STEP,
                                        budget);

                        if (stepDecision.isDenied()) {

                                return finishWithFallback(
                                                current,
                                                stepDecision.reason(),
                                                publisher);
                        }

                        /*
                         * =================================================
                         * 2. CONTEXT Resource Guardrail
                         * =================================================
                         *
                         * 在下一次 Action Selection / LLM 调用之前，
                         * 检查当前 Agent Context 的近似字符规模。
                         *
                         * Context 一旦超限，不再调用模型生成 fallback，
                         * 避免使用已经超大的 Context 再触发一次 LLM 调用。
                         */
                        int contextLength = contextSizeEstimator.estimate(
                                        current);

                        RuntimeResourceResult contextDecision = executionPolicy.evaluateResource(
                                        RuntimeResourceType.CONTEXT,
                                        contextLength,
                                        null);

                        if (contextDecision.isDenied()) {

                                return finishWithoutModel(
                                                current,
                                                contextDecision.reason());
                        }

                        /*
                         * =================================================
                         * 3. Resolve current Task
                         * =================================================
                         */
                        AgentTask pendingTask = current.getAgentPlan()
                                        .nextPendingTask()
                                        .orElseThrow();

                        /*
                         * =================================================
                         * 4. PENDING → RUNNING
                         * =================================================
                         */
                        current = updateTaskStatus(
                                        current,
                                        pendingTask.getId(),
                                        AgentTaskStatus.RUNNING);

                        AgentTask runningTask = current.getAgentPlan()
                                        .findTaskById(
                                                        pendingTask.getId())
                                        .orElseThrow();

                        publisher.publish(
                                        AgentStreamEvent.task(
                                                        AgentStreamEventType.TASK_STARTED,
                                                        runningTask.getId(),
                                                        runningTask.getDescription()));

                        /*
                         * =================================================
                         * 5. Action Selection
                         * =================================================
                         */

                        AgentAction action = actionCoordinator.selectAction(
                                        current,
                                        runningTask);

                        /*
                         * =================================================
                         * 6. TOOL_CALL Guardrail
                         * =================================================
                         *
                         * 当前定义：
                         *
                         * maxToolCalls 实际约束的是 Tool Action Attempt。
                         *
                         * 即使后面 ToolAuthorization DENY，
                         * 这次由 Agent 发起的 Tool Action 仍然消耗一次 Runtime
                         * Tool Budget。
                         *
                         * 这是更保守的安全模型。
                         */
                        if (action.isTool()) {

                                RuntimeGuardrailResult toolCallDecision = executionPolicy.evaluateRuntime(
                                                RuntimeGuardrailOperation.TOOL_CALL,
                                                budget);

                                if (toolCallDecision.isDenied()) {

                                        return finishWithFallback(
                                                        current,
                                                        toolCallDecision.reason(),
                                                        publisher);
                                }
                        }

                        /*
                         * =================================================
                         * 7. Action Execution
                         * =================================================
                         */
                        if (action.isTool()) {

                                publisher.publish(
                                                AgentStreamEvent.task(
                                                                AgentStreamEventType.TOOL_STARTED,
                                                                runningTask.getId(),
                                                                "正在执行工具调用"));
                        }

                        if (action.isReason()) {

                                publisher.publish(
                                                AgentStreamEvent.task(
                                                                AgentStreamEventType.REASONING_STARTED,
                                                                runningTask.getId(),
                                                                "正在分析当前任务"));
                        }

                        AgentActionExecutionResult result = actionCoordinator.executeAction(
                                        current,
                                        runningTask,
                                        action);

                        Objects.requireNonNull(
                                        result,
                                        "AgentActionExecutionResult must not be null");

                        /*
                         * Tool Action 已经越过 Runtime Execution Boundary。
                         *
                         * 无论最终：
                         *
                         * - Tool Success
                         * - Tool Failure
                         * - Tool Authorization DENY
                         *
                         * 当前版本都记为一次 Tool Action Attempt。
                         */
                        if (action.isTool()) {

                                publisher.publish(
                                                AgentStreamEvent.task(
                                                                AgentStreamEventType.TOOL_COMPLETED,
                                                                runningTask.getId(),
                                                                "工具调用完成"));

                                budget.recordToolCall();
                        }

                        if (action.isReason()) {

                                publisher.publish(
                                                AgentStreamEvent.task(
                                                                AgentStreamEventType.REASONING_COMPLETED,
                                                                runningTask.getId(),
                                                                "当前任务分析完成"));
                        }

                        /*
                         * 一个完整 Runtime Execution Cycle 已经发生。
                         */
                        budget.recordStep();

                        /*
                         * =================================================
                         * 8. Observe / Apply
                         * =================================================
                         */
                        current = applyExecutionResult(
                                        current,
                                        runningTask,
                                        result);

                        /*
                         * FINAL_ANSWER 是 Terminal Action。
                         *
                         * Final Answer 已经生成，
                         * 无需再次 Reflection。
                         */
                        if (current.hasFinalAnswer()) {

                                return markFinished(
                                                current);
                        }

                        /*
                         * =================================================
                         * 9. Reflection
                         * =================================================
                         */

                        publisher.publish(
                                        AgentStreamEvent.task(
                                                        AgentStreamEventType.REFLECTION_STARTED,
                                                        runningTask.getId(),
                                                        "正在评估当前任务结果"));

                        ReflectionResult reflectionResult = reflectionService.reflect(
                                        current,
                                        runningTask);

                        Objects.requireNonNull(
                                        reflectionResult,
                                        "ReflectionResult must not be null");

                        publisher.publish(
                                        AgentStreamEvent.task(
                                                        AgentStreamEventType.REFLECTION_COMPLETED,
                                                        runningTask.getId(),
                                                        "当前任务结果评估完成"));

                        /*
                         * =================================================
                         * 10. RETRY Guardrail
                         * =================================================
                         *
                         * 新模型：
                         *
                         * Retry Budget 属于整个 Agent Execution，
                         * 不再是 per-task retry counter。
                         */
                        /*
                         * =================================================
                         * 10. RETRY Guardrail + No-progress Detection
                         * =================================================
                         */
                        if (reflectionResult.shouldRetry()) {

                                /*
                                 * 如果同一 Task / Tool 已经连续拿到完全相同的
                                 * successful Evidence，
                                 * 再次 RETRY 不可能产生新信息。
                                 *
                                 * 此时禁止继续 Tool retry loop。
                                 */
                                if (noProgressRetryPolicy.isNoProgress(
                                                current,
                                                runningTask)) {

                                        executionObserver.recordNoProgressSuppression();

                                        log.warn(
                                                        "No-progress retry suppressed. taskId={}, reason=IDENTICAL_TOOL_EVIDENCE",
                                                        runningTask.getId());

                                        /*
                                         * 当前 Task 已经至少获得有效 Tool Evidence，
                                         * 只是 Reflection 认为证据“不够完美”。
                                         *
                                         * 不再重复 Tool，而是把当前 Task 视为已经获得
                                         * best-effort evidence，让后续 REASON / FINAL ANSWER
                                         * 基于现有 Evidence 完成。
                                         */
                                        reflectionResult = ReflectionResult.of(
                                                        com.quince.lawyeraiassistant.agent.model.ReflectionDecision.CONTINUE,
                                                        "Retry suppressed because repeated tool execution produced identical evidence. "
                                                                        + "Continue with the best available evidence.");

                                } else {

                                        RuntimeGuardrailResult retryDecision = executionPolicy.evaluateRuntime(
                                                        RuntimeGuardrailOperation.RETRY,
                                                        budget);

                                        if (retryDecision.isDenied()) {

                                                return finishWithFallback(
                                                                current,
                                                                retryDecision.reason(),
                                                                publisher);
                                        }

                                        budget.recordRetry();

                                        executionObserver.recordRetry();
                                }
                        }

                        /*
                         * =================================================
                         * 11. REPLAN Guardrail
                         * =================================================
                         */
                        if (reflectionResult.shouldReplan()) {

                                RuntimeGuardrailResult replanDecision = executionPolicy.evaluateRuntime(
                                                RuntimeGuardrailOperation.REPLAN,
                                                budget);

                                if (replanDecision.isDenied()) {

                                        return finishWithFallback(
                                                        current,
                                                        replanDecision.reason(),
                                                        publisher);
                                }

                                budget.recordReplan();
                        }

                        /*
                         * =================================================
                         * 12. Apply Reflection Decision
                         * =================================================
                         */
                        current = handleReflectionDecision(
                                        current,
                                        runningTask,
                                        reflectionResult,
                                        publisher);

                        /*
                         * FINISH decision is terminal.
                         */
                        if (current.getStatus() == AgentStatus.FINISHED) {

                                return current;
                        }
                }

                /*
                 * =====================================================
                 * All Pending Tasks completed
                 * =====================================================
                 *
                 * Plan 已经执行结束，
                 * 但可能还没有显式 FINAL_ANSWER Action。
                 */
                current = ensureFinalAnswer(
                                current,
                                publisher);

                return markFinished(
                                current);
        }

        /*
         * =========================================================
         * Skill
         * =========================================================
         */

        private AgentContext attachSelectedSkill(
                        AgentContext context) {

                if (context.hasSkill()) {

                        return context;
                }

                return skillSelector
                                .select(
                                                context.getGoal())
                                .map(
                                                skill -> context
                                                                .withSkillContext(
                                                                                SkillContext.of(
                                                                                                skill))
                                                                .appendExecutionLog(
                                                                                "Skill selected: "
                                                                                                + skill.getId()))
                                .orElseGet(
                                                () -> context.appendExecutionLog(
                                                                "No skill selected"));
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

                EvidenceValidationResult evidenceDecision = evidencePolicy.validate(
                                task,
                                observation);

                if (!evidenceDecision.accepted()) {

                        return updateTaskStatus(
                                        context,
                                        task.getId(),
                                        AgentTaskStatus.FAILED)
                                        .appendExecutionLog(
                                                        evidenceDecision.reason());
                }

                Objects.requireNonNull(
                                observation,
                                "ToolObservation must not be null");

                int observationLength = contextSizeEstimator.estimateObservation(
                                observation);

                RuntimeResourceResult resourceDecision = executionPolicy.evaluateResource(
                                RuntimeResourceType.OBSERVATION,
                                observationLength,
                                task.getId());

                if (resourceDecision.isDenied()) {

                        ToolObservation protectedObservation = ToolObservation.failure(
                                        task.getId(),
                                        observation.getToolName(),
                                        resourceDecision.reason(),
                                        LegalSecurityContext.of(
                                                        SecuritySource.RUNTIME,
                                                        SecurityTrustLevel.DERIVED));

                        return updateTaskStatus(
                                        context,
                                        task.getId(),
                                        AgentTaskStatus.FAILED)
                                        .appendObservation(
                                                        protectedObservation)
                                        .appendExecutionLog(
                                                        "Runtime resource guardrail triggered: "
                                                                        + resourceDecision.reason());
                }

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

                int observationLength = contextSizeEstimator.estimateText(
                                content);

                RuntimeResourceResult resourceDecision = executionPolicy.evaluateResource(
                                RuntimeResourceType.OBSERVATION,
                                observationLength,
                                task.getId());

                if (resourceDecision.isDenied()) {

                        return updateTaskStatus(
                                        context,
                                        task.getId(),
                                        AgentTaskStatus.FAILED)
                                        .appendExecutionLog(
                                                        "Runtime resource guardrail triggered: "
                                                                        + resourceDecision.reason());
                }

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
                        ReflectionResult reflectionResult,
                        AgentStreamPublisher publisher) {

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
                                                reflectionResult,
                                                publisher);
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
                        ReflectionResult reflectionResult,
                        AgentStreamPublisher publisher) {

                AgentContext updated = ensureFinalAnswer(
                                context,
                                publisher)
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
                        String reason,
                        AgentStreamPublisher publisher) {

                AgentContext updated = context.appendExecutionLog(
                                "Runtime guardrail triggered: "
                                                + reason);

                updated = ensureFinalAnswer(
                                updated,
                                publisher);

                return markFinished(
                                updated);
        }

        private AgentContext finishWithoutModel(
                        AgentContext context,
                        String reason) {

                AgentContext updated = context.appendExecutionLog(
                                "Runtime resource guardrail triggered: "
                                                + reason);

                if (!updated.hasFinalAnswer()) {

                        updated = updated.withFinalAnswer(
                                        "Agent 执行已停止：运行时资源限制已达到。");
                }

                return markFinished(
                                updated);
        }

        /*
         * =========================================================
         * Runtime Helpers
         * =========================================================
         */

        private AgentContext ensureFinalAnswer(
                        AgentContext context,
                        AgentStreamPublisher publisher) {

                if (context.hasFinalAnswer()) {

                        return context;
                }

                publisher.publish(
                                AgentStreamEvent.of(
                                                AgentStreamEventType.FINAL_ANSWER_STARTED,
                                                "正在生成最终答案"));

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

        /*
         * =========================================================
         * Performance Baseline
         * =========================================================
         */

        private <T> T measureStage(
                        String stage,
                        java.util.function.Supplier<T> operation) {

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
