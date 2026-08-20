package com.quince.lawyeraiassistant.agent.runtime;

import com.quince.lawyeraiassistant.agent.action.AgentActionSelector;
import com.quince.lawyeraiassistant.agent.action.policy.DuplicateToolCallPolicy;
import com.quince.lawyeraiassistant.agent.action.policy.NoProgressRetryPolicy;
import com.quince.lawyeraiassistant.agent.action.routing.DeterministicActionRouter;
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
import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceContext;
import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceMetrics;
import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceSnapshot;
import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceSnapshotRecorder;
import com.quince.lawyeraiassistant.agent.runtime.metrics.micrometer.AgentMicrometerMetrics;
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
import com.quince.lawyeraiassistant.security.audit.SecurityAuditEvent;
import com.quince.lawyeraiassistant.security.audit.SecurityAuditEventType;
import com.quince.lawyeraiassistant.security.audit.SecurityAuditLogger;
import com.quince.lawyeraiassistant.security.legal.LegalSecurityContext;
import com.quince.lawyeraiassistant.security.legal.SecuritySource;
import com.quince.lawyeraiassistant.security.legal.SecurityTrustLevel;
import com.quince.lawyeraiassistant.security.legal.evidence.LegalEvidenceTrustPolicy;
import com.quince.lawyeraiassistant.security.runtime.AgentExecutionBudget;
import com.quince.lawyeraiassistant.security.runtime.AgentExecutionLimits;
import com.quince.lawyeraiassistant.security.runtime.RuntimeGuardrailOperation;
import com.quince.lawyeraiassistant.security.runtime.RuntimeGuardrailResult;
import com.quince.lawyeraiassistant.security.runtime.RuntimeGuardrailService;
import com.quince.lawyeraiassistant.security.runtime.performance.PerformanceGuardrailService;
import com.quince.lawyeraiassistant.security.runtime.performance.PerformanceGuardrailResult;
import com.quince.lawyeraiassistant.security.runtime.resource.RuntimeResourceGuardrailService;
import com.quince.lawyeraiassistant.security.runtime.resource.RuntimeResourceResult;
import com.quince.lawyeraiassistant.security.runtime.resource.RuntimeResourceType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Component
public class DefaultAgentRuntime
                implements AgentRuntime {

        private static final Logger log = LoggerFactory.getLogger(
                        DefaultAgentRuntime.class);

        private static final String AGENT_FINISHED_LOG = "Agent finished";

        private final AgentPipeline agentPipeline;

        private final AgentActionSelector actionSelector;

        private final AgentActionExecutionOperator actionExecutionOperator;

        private final AgentReflectionService reflectionService;

        private final AgentReplanningService replanningService;

        private final AgentFinalAnswerService finalAnswerService;

        private final AgentSkillSelector skillSelector;

        private final AgentExecutionLimits executionLimits;

        private final RuntimeGuardrailService runtimeGuardrailService;

        private final RuntimeResourceGuardrailService runtimeResourceGuardrailService;

        private final LegalEvidenceTrustPolicy legalEvidenceTrustPolicy;

        private final SecurityAuditLogger securityAuditLogger;

        private final DuplicateToolCallPolicy duplicateToolCallPolicy;

        private final DeterministicActionRouter deterministicActionRouter;

        private final NoProgressRetryPolicy noProgressRetryPolicy;

        private final AgentPerformanceContext performanceContext;

        private final PerformanceGuardrailService performanceGuardrailService;

        private final AgentMicrometerMetrics micrometerMetrics;

        private final AgentPerformanceSnapshotRecorder performanceSnapshotRecorder;

        public DefaultAgentRuntime(
                        AgentPipeline agentPipeline,
                        AgentSkillSelector skillSelector,
                        AgentActionSelector actionSelector,
                        AgentActionExecutionOperator actionExecutionOperator,
                        AgentReflectionService reflectionService,
                        AgentReplanningService replanningService,
                        AgentFinalAnswerService finalAnswerService,
                        AgentExecutionLimits executionLimits,
                        RuntimeGuardrailService runtimeGuardrailService,
                        RuntimeResourceGuardrailService runtimeResourceGuardrailService,
                        LegalEvidenceTrustPolicy legalEvidenceTrustPolicy,
                        SecurityAuditLogger securityAuditLogger,
                        DuplicateToolCallPolicy duplicateToolCallPolicy,
                        DeterministicActionRouter deterministicActionRouter,
                        NoProgressRetryPolicy noProgressRetryPolicy,
                        AgentPerformanceContext performanceContext,
                        PerformanceGuardrailService performanceGuardrailService,
                        AgentMicrometerMetrics micrometerMetrics,
                        AgentPerformanceSnapshotRecorder performanceSnapshotRecorder) {

                this.agentPipeline = Objects.requireNonNull(
                                agentPipeline,
                                "agentPipeline must not be null");

                this.skillSelector = Objects.requireNonNull(
                                skillSelector,
                                "skillSelector must not be null");

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

                this.executionLimits = Objects.requireNonNull(
                                executionLimits,
                                "executionLimits must not be null");

                this.runtimeGuardrailService = Objects.requireNonNull(
                                runtimeGuardrailService,
                                "runtimeGuardrailService must not be null");

                this.runtimeResourceGuardrailService = Objects.requireNonNull(
                                runtimeResourceGuardrailService,
                                "runtimeResourceGuardrailService must not be null");

                this.legalEvidenceTrustPolicy = Objects.requireNonNull(
                                legalEvidenceTrustPolicy,
                                "legalEvidenceTrustPolicy must not be null");

                this.securityAuditLogger = Objects.requireNonNull(
                                securityAuditLogger,
                                "securityAuditLogger must not be null");

                this.duplicateToolCallPolicy = Objects.requireNonNull(
                                duplicateToolCallPolicy,
                                "duplicateToolCallPolicy must not be null");

                this.deterministicActionRouter = Objects.requireNonNull(
                                deterministicActionRouter,
                                "deterministicActionRouter must not be null");

                this.noProgressRetryPolicy = Objects.requireNonNull(
                                noProgressRetryPolicy,
                                "noProgressRetryPolicy must not be null");

                this.performanceContext = Objects.requireNonNull(
                                performanceContext,
                                "AgentPerformanceContext must not be null");

                this.performanceGuardrailService = Objects.requireNonNull(
                                performanceGuardrailService,
                                "PerformanceGuardrailService must not be null");

                this.micrometerMetrics = Objects.requireNonNull(
                                micrometerMetrics,
                                "AgentMicrometerMetrics must not be null");

                this.performanceSnapshotRecorder = Objects.requireNonNull(
                                performanceSnapshotRecorder,
                                "AgentPerformanceSnapshotRecorder must not be null");
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

                long startedAt = System.nanoTime();

                AgentContext result = null;

                RuntimeExecutionStatus executionStatus = RuntimeExecutionStatus.FAILED;

                AgentPerformanceMetrics performanceMetrics = performanceContext.start();

                try {

                        publisher.publish(
                                        AgentStreamEvent.of(
                                                        AgentStreamEventType.AGENT_STARTED,
                                                        "Agent execution started"));

                        result = runInternal(
                                        context,
                                        publisher);

                        executionStatus = RuntimeExecutionStatus.SUCCESS;

                        return result;

                } catch (RuntimeException exception) {

                        publisher.publish(
                                        AgentStreamEvent.of(
                                                        AgentStreamEventType.AGENT_FAILED,
                                                        "Agent execution failed"));

                        throw exception;

                } finally {

                        long durationMs = TimeUnit.NANOSECONDS.toMillis(
                                        System.nanoTime()
                                                        - startedAt);

                        logAgentExecutionFinished(
                                        context,
                                        result,
                                        executionStatus,
                                        durationMs);

                        try {

                                AgentPerformanceSnapshot snapshot = performanceMetrics.snapshot();

                                performanceSnapshotRecorder.record(
                                                snapshot);

                                logPerformanceSummary(
                                                snapshot);

                                micrometerMetrics.recordExecution(
                                                snapshot);

                                List<PerformanceGuardrailResult> guardrailResults = performanceGuardrailService
                                                .evaluate(
                                                                snapshot);

                                guardrailResults.forEach(
                                                this::logPerformanceGuardrailResult);

                                micrometerMetrics.recordGuardrailResults(
                                                guardrailResults);

                        } finally {

                                performanceContext.clear();
                        }
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
                AgentExecutionBudget budget = new AgentExecutionBudget(
                                executionLimits);

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
                        RuntimeGuardrailResult stepDecision = runtimeGuardrailService.evaluate(
                                        RuntimeGuardrailOperation.STEP,
                                        budget);

                        if (stepDecision.isDenied()) {

                                auditRuntimeLimit(
                                                RuntimeGuardrailOperation.STEP,
                                                stepDecision);

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
                        int contextLength = estimateContextLength(
                                        current);

                        RuntimeResourceResult contextDecision = runtimeResourceGuardrailService.evaluate(
                                        RuntimeResourceType.CONTEXT,
                                        contextLength);

                        if (contextDecision.isDenied()) {

                                auditResourceLimit(
                                                RuntimeResourceType.CONTEXT,
                                                contextLength,
                                                contextDecision,
                                                null);

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

                        AgentAction action = resolveAction(
                                        current,
                                        runningTask);

                        Objects.requireNonNull(
                                        action,
                                        "AgentAction must not be null");

                        action = applyDuplicateToolCallGuard(
                                        current,
                                        runningTask,
                                        action);

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

                                RuntimeGuardrailResult toolCallDecision = runtimeGuardrailService.evaluate(
                                                RuntimeGuardrailOperation.TOOL_CALL,
                                                budget);

                                if (toolCallDecision.isDenied()) {

                                        auditRuntimeLimit(
                                                        RuntimeGuardrailOperation.TOOL_CALL,
                                                        toolCallDecision);

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

                        AgentActionExecutionResult result = actionExecutionOperator.execute(
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

                                        performanceContext.current()
                                                        .ifPresent(
                                                                        AgentPerformanceMetrics::recordNoProgressSuppression);

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

                                        RuntimeGuardrailResult retryDecision = runtimeGuardrailService.evaluate(
                                                        RuntimeGuardrailOperation.RETRY,
                                                        budget);

                                        if (retryDecision.isDenied()) {

                                                auditRuntimeLimit(
                                                                RuntimeGuardrailOperation.RETRY,
                                                                retryDecision);

                                                return finishWithFallback(
                                                                current,
                                                                retryDecision.reason(),
                                                                publisher);
                                        }

                                        budget.recordRetry();

                                        performanceContext
                                                        .current()
                                                        .ifPresent(
                                                                        metrics -> metrics.recordRetry());
                                }
                        }

                        /*
                         * =================================================
                         * 11. REPLAN Guardrail
                         * =================================================
                         */
                        if (reflectionResult.shouldReplan()) {

                                RuntimeGuardrailResult replanDecision = runtimeGuardrailService.evaluate(
                                                RuntimeGuardrailOperation.REPLAN,
                                                budget);

                                if (replanDecision.isDenied()) {

                                        auditRuntimeLimit(
                                                        RuntimeGuardrailOperation.REPLAN,
                                                        replanDecision);

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

                try {

                        legalEvidenceTrustPolicy.validate(
                                        observation);

                } catch (RuntimeException exception) {

                        securityAuditLogger.log(
                                        SecurityAuditEvent.warn(
                                                        SecurityAuditEventType.EVIDENCE_TRUST_REJECTED,
                                                        "DefaultAgentRuntime",
                                                        "Evidence trust boundary rejected tool result",
                                                        Map.of(
                                                                        "taskId",
                                                                        task.getId(),
                                                                        "toolName",
                                                                        observation == null
                                                                                        ? "unknown"
                                                                                        : observation.getToolName())));

                        return updateTaskStatus(
                                        context,
                                        task.getId(),
                                        AgentTaskStatus.FAILED)
                                        .appendExecutionLog(
                                                        "Evidence trust boundary rejected tool result");
                }

                Objects.requireNonNull(
                                observation,
                                "ToolObservation must not be null");

                int observationLength = resolveObservationLength(
                                observation);

                RuntimeResourceResult resourceDecision = runtimeResourceGuardrailService.evaluate(
                                RuntimeResourceType.OBSERVATION,
                                observationLength);

                if (resourceDecision.isDenied()) {

                        auditResourceLimit(
                                        RuntimeResourceType.OBSERVATION,
                                        observationLength,
                                        resourceDecision,
                                        task.getId());

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

                int observationLength = lengthOf(
                                content);

                RuntimeResourceResult resourceDecision = runtimeResourceGuardrailService.evaluate(
                                RuntimeResourceType.OBSERVATION,
                                observationLength);

                if (resourceDecision.isDenied()) {

                        auditResourceLimit(
                                        RuntimeResourceType.OBSERVATION,
                                        observationLength,
                                        resourceDecision,
                                        task.getId());

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

        private int resolveObservationLength(
                        ToolObservation observation) {

                if (observation.isSuccess()) {

                        String content = observation.getContent();

                        return content == null
                                        ? 0
                                        : content.length();
                }

                String errorMessage = observation.getErrorMessage();

                return errorMessage == null
                                ? 0
                                : errorMessage.length();
        }

        private int estimateContextLength(
                        AgentContext context) {

                Objects.requireNonNull(
                                context,
                                "AgentContext must not be null");

                long length = 0L;

                length += lengthOf(
                                context.getGoal());

                if (context.getReasonResult() != null) {

                        length += lengthOf(
                                        context.getReasonResult()
                                                        .getReasonSummary());
                }

                for (AgentTask task : context.getAgentPlan()
                                .getTasks()) {

                        length += lengthOf(
                                        task.getId());

                        length += lengthOf(
                                        task.getDescription());

                        if (task.getStatus() != null) {

                                length += task.getStatus()
                                                .name()
                                                .length();
                        }
                }

                for (ToolObservation observation : context.getObservations()) {

                        length += lengthOf(
                                        observation.getTaskId());

                        length += lengthOf(
                                        observation.getToolName());

                        length += resolveObservationLength(
                                        observation);
                }

                for (RuntimeReasonObservation observation : context.getRuntimeReasonObservations()) {

                        length += lengthOf(
                                        observation.getTaskId());

                        length += lengthOf(
                                        observation.getContent());
                }

                for (String executionLog : context.getExecutionLogs()) {

                        length += lengthOf(
                                        executionLog);
                }

                length += lengthOf(
                                context.getFinalAnswer());

                if (context.hasSkill()) {

                        SkillContext skillContext = context.getSkillContext()
                                        .orElseThrow();

                        length += lengthOf(
                                        skillContext.getSkillId());

                        length += lengthOf(
                                        skillContext.getSkillName());

                        length += lengthOf(
                                        skillContext.getDescription());

                        length += lengthOf(
                                        skillContext.getInstructions());

                        for (String toolName : skillContext.getAllowedTools()) {

                                length += lengthOf(
                                                toolName);
                        }
                }

                return length > Integer.MAX_VALUE
                                ? Integer.MAX_VALUE
                                : (int) length;
        }

        private int lengthOf(
                        String value) {

                return value == null
                                ? 0
                                : value.length();
        }

        private void auditRuntimeLimit(
                        RuntimeGuardrailOperation operation,
                        RuntimeGuardrailResult result) {

                securityAuditLogger.log(
                                SecurityAuditEvent.warn(
                                                SecurityAuditEventType.RUNTIME_LIMIT_REACHED,
                                                "DefaultAgentRuntime",
                                                result.reason(),
                                                Map.of(
                                                                "operation",
                                                                operation.name(),
                                                                "policyName",
                                                                result.policyName())));
        }

        private void auditResourceLimit(
                        RuntimeResourceType resourceType,
                        int actualLength,
                        RuntimeResourceResult result,
                        String taskId) {

                Map<String, String> metadata = new java.util.HashMap<>();

                metadata.put(
                                "resourceType",
                                resourceType.name());

                metadata.put(
                                "actualLength",
                                String.valueOf(
                                                actualLength));

                metadata.put(
                                "policyName",
                                result.policyName());

                if (taskId != null
                                && !taskId.isBlank()) {

                        metadata.put(
                                        "taskId",
                                        taskId);
                }

                securityAuditLogger.log(
                                SecurityAuditEvent.warn(
                                                SecurityAuditEventType.RESOURCE_LIMIT_REACHED,
                                                "DefaultAgentRuntime",
                                                result.reason(),
                                                metadata));
        }

        /*
         * =========================================================
         * Performance Baseline
         * =========================================================
         */

        private void logAgentExecutionFinished(
                        AgentContext initialContext,
                        AgentContext result,
                        RuntimeExecutionStatus executionStatus,
                        long durationMs) {

                String tenantId = initialContext.getTenantContext() == null
                                ? "unknown"
                                : initialContext.getTenantContext()
                                                .tenantId();

                AgentStatus agentStatus = result == null
                                ? null
                                : result.getStatus();

                log.info(
                                "Agent execution finished. tenantId={}, executionStatus={}, agentStatus={}, durationMs={}",
                                tenantId,
                                executionStatus,
                                agentStatus,
                                durationMs);
        }

        private void logPerformanceSummary(
                        AgentPerformanceSnapshot snapshot) {

                log.info(
                                "Agent performance summary. "
                                                + "totalDurationMs={}, "
                                                + "llmCalls={}, "
                                                + "llmDurationMs={}, "
                                                + "llmRatio={}, "
                                                + "toolCalls={}, "
                                                + "toolDurationMs={}, "
                                                + "mcpCalls={}, "
                                                + "mcpDurationMs={}, "
                                                + "cacheHits={}, "
                                                + "cacheMisses={}, "
                                                + "cacheHitRatio={}, "
                                                + "evidenceOriginalChars={}, "
                                                + "evidenceCompactedChars={}, "
                                                + "evidenceReductionRatio={}, "
                                                + "retries={}, "
                                                + "noProgressSuppressions={}",
                                snapshot.totalDurationMs(),
                                snapshot.llmCalls(),
                                snapshot.llmDurationMs(),
                                snapshot.llmDurationRatio(),
                                snapshot.toolCalls(),
                                snapshot.toolDurationMs(),
                                snapshot.mcpCalls(),
                                snapshot.mcpDurationMs(),
                                snapshot.cacheHits(),
                                snapshot.cacheMisses(),
                                snapshot.cacheHitRatio(),
                                snapshot.evidenceOriginalChars(),
                                snapshot.evidenceCompactedChars(),
                                snapshot.evidenceReductionRatio(),
                                snapshot.retries(),
                                snapshot.noProgressSuppressions());
        }

        private void logPerformanceGuardrailResult(
                        PerformanceGuardrailResult result) {

                if (result.isCritical()) {

                        log.error(
                                        "Agent performance guardrail critical. metric={}, actual={}, threshold={}, reason={}",
                                        result.metric(),
                                        result.actual(),
                                        result.threshold(),
                                        result.reason());

                } else if (result.isWarn()) {

                        log.warn(
                                        "Agent performance guardrail warning. metric={}, actual={}, threshold={}, reason={}",
                                        result.metric(),
                                        result.actual(),
                                        result.threshold(),
                                        result.reason());
                }
        }

        private enum RuntimeExecutionStatus {

                SUCCESS,

                FAILED
        }

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

        private AgentAction applyDuplicateToolCallGuard(
                        AgentContext context,
                        AgentTask currentTask,
                        AgentAction action) {

                if (action == null
                                || !action.isTool()) {

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
                                                () -> {

                                                        AgentContext actionSelectionContext = context;

                                                        return measureStage(
                                                                        "ACTION_SELECTION",
                                                                        () -> actionSelector.select(
                                                                                        actionSelectionContext,
                                                                                        task));
                                                });
        }

}
