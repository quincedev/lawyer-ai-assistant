package com.quince.lawyeraiassistant.agent.model;

import com.quince.lawyeraiassistant.agent.skill.AgentSkill;
import com.quince.lawyeraiassistant.agent.skill.context.SkillContext;
import com.quince.lawyeraiassistant.security.legal.LegalSecurityContext;
import com.quince.lawyeraiassistant.security.legal.SecuritySource;
import com.quince.lawyeraiassistant.security.legal.SecurityTrustLevel;
import com.quince.lawyeraiassistant.security.tenant.TenantContext;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Agent Pipeline 的统一上下文对象。
 *
 * <p>
 * 负责保存一次 Agent 执行过程中的核心状态。
 * </p>
 *
 * <p>
 * 当前版本包含：
 * </p>
 *
 * <ul>
 * <li>goal：Agent 需要完成的目标</li>
 * <li>reasonResult：Reason 阶段产生的推理摘要结果</li>
 * <li>agentPlan：Planning 阶段产生的执行计划</li>
 * <li>observations：Tool 执行产生的观察结果</li>
 * <li>status：Agent 当前执行状态</li>
 * <li>executionLogs：Agent 执行过程中的结构化日志摘要</li>
 * </ul>
 *
 * <p>
 * 本对象采用不可变设计。AgentOperator 不直接修改当前实例，
 * 而是通过 {@link #toBuilder()} 或辅助方法创建新的 AgentContext。
 * </p>
 */
@Getter
@ToString
@EqualsAndHashCode
public final class AgentContext {

        /**
         * Agent 需要完成的目标。
         */
        private final String goal;

        /**
         * Reason 阶段产生的结果。
         *
         * <p>
         * 初始 AgentContext 尚未执行 Reason，因此允许为 null。
         * </p>
         */
        private final ReasonResult reasonResult;

        /**
         * Planning 阶段产生的执行计划。
         *
         * <p>
         * 初始状态统一使用空计划，不使用 null。
         * </p>
         */
        private final AgentPlan agentPlan;

        /**
         * Tool 执行产生的 Observation。
         *
         * <p>
         * 初始状态统一使用空集合，不使用 null。
         * </p>
         */
        private final List<ToolObservation> observations;

        /**
         * Agent 当前执行状态。
         */
        private final AgentStatus status;

        /**
         * Agent 执行日志。
         *
         * <p>
         * 这里只保存结构化执行摘要，
         * 不保存 Tool 的业务返回结果。
         * Tool 业务结果统一保存在 observations 中。
         * </p>
         */
        private final List<String> executionLogs;

        private final String finalAnswer;

        private final List<RuntimeReasonObservation> runtimeReasonObservations;

        private final SkillContext skillContext;

        private final LegalSecurityContext legalSecurityContext;

        private final TenantContext tenantContext;

        @Builder(toBuilder = true)
        private AgentContext(
                        String goal,
                        ReasonResult reasonResult,
                        AgentPlan agentPlan,
                        List<ToolObservation> observations,
                        AgentStatus status,
                        List<String> executionLogs,
                        String finalAnswer,
                        List<RuntimeReasonObservation> runtimeReasonObservations,
                        SkillContext skillContext,
                        LegalSecurityContext legalSecurityContext,
                        TenantContext tenantContext) {

                this.goal = normalizeGoal(
                                goal);

                this.reasonResult = reasonResult;

                this.agentPlan = agentPlan == null
                                ? AgentPlan.empty()
                                : agentPlan;

                this.observations = normalizeObservations(
                                observations);

                this.status = status == null
                                ? AgentStatus.CREATED
                                : status;

                this.executionLogs = normalizeExecutionLogs(
                                executionLogs);

                this.finalAnswer = normalizeFinalAnswer(finalAnswer);

                this.runtimeReasonObservations = normalizeRuntimeReasonObservations(runtimeReasonObservations);

                this.skillContext = skillContext;

                this.legalSecurityContext = legalSecurityContext;

                this.tenantContext = tenantContext;
        }

        /**
         * 根据 Goal 创建初始 AgentContext。
         *
         * <p>
         * 初始状态：
         * </p>
         *
         * <ul>
         * <li>status = CREATED</li>
         * <li>reasonResult = null</li>
         * <li>agentPlan = empty plan</li>
         * <li>observations = empty list</li>
         * <li>executionLogs = empty list</li>
         * </ul>
         *
         * @param goal Agent 目标
         * @return 初始 AgentContext
         */
        public static AgentContext from(
                        String goal) {

                return AgentContext.builder()
                                .goal(
                                                goal)
                                .legalSecurityContext(
                                                LegalSecurityContext.of(
                                                                SecuritySource.USER_INPUT,
                                                                SecurityTrustLevel.UNTRUSTED))
                                .build();
        }

        public static AgentContext authenticated(
                        String goal,
                        TenantContext tenantContext) {

                Objects.requireNonNull(
                                tenantContext,
                                "TenantContext must not be null");

                return AgentContext.builder()
                                .goal(
                                                goal)
                                .legalSecurityContext(
                                                LegalSecurityContext.of(
                                                                SecuritySource.USER_INPUT,
                                                                SecurityTrustLevel.UNTRUSTED))
                                .tenantContext(
                                                tenantContext)
                                .build();
        }

        public TenantContext requireTenantContext() {

                if (tenantContext == null) {

                        throw new IllegalStateException(
                                        "TenantContext is required for authenticated Agent execution");
                }

                return tenantContext;
        }

        /**
         * 判断 Agent 是否正在执行。
         */
        public boolean isRunning() {
                return status == AgentStatus.RUNNING;
        }

        /**
         * 判断 Agent 是否已经成功完成。
         */
        public boolean isFinished() {
                return status == AgentStatus.FINISHED;
        }

        /**
         * 判断 Agent 是否执行失败。
         */
        public boolean isFailed() {
                return status == AgentStatus.FAILED;
        }

        /**
         * 判断当前上下文是否已经包含 Reason 结果。
         */
        public boolean hasReasonResult() {
                return reasonResult != null;
        }

        /**
         * 判断当前上下文是否已经包含有效执行计划。
         */
        public boolean hasAgentPlan() {
                return agentPlan.hasTasks();
        }

        /**
         * 判断当前是否已经产生 Observation。
         */
        public boolean hasObservations() {
                return !observations.isEmpty();
        }

        /**
         * 返回 Observation 数量。
         */
        public int observationCount() {
                return observations.size();
        }

        /**
         * 判断当前是否包含执行日志。
         */
        public boolean hasExecutionLogs() {
                return !executionLogs.isEmpty();
        }

        public Optional<SkillContext> getSkillContext() {
                return Optional.ofNullable(skillContext);
        }

        public boolean hasSkill() {
                return skillContext != null;
        }

        public Optional<AgentSkill> getSelectedSkill() {
                return getSkillContext()
                                .map(SkillContext::getSkill);
        }

        public AgentContext withSkillContext(
                        SkillContext skillContext) {

                Objects.requireNonNull(
                                skillContext,
                                "SkillContext must not be null");

                return toBuilder()
                                .skillContext(skillContext)
                                .build();
        }

        /**
         * 返回执行日志数量。
         */
        public int executionLogCount() {
                return executionLogs.size();
        }

        /**
         * 创建包含 ReasonResult 的新 AgentContext。
         */
        public AgentContext withReasonResult(
                        ReasonResult reasonResult) {

                Objects.requireNonNull(
                                reasonResult,
                                "ReasonResult must not be null");

                return toBuilder()
                                .reasonResult(reasonResult)
                                .build();
        }

        /**
         * 创建包含 AgentPlan 的新 AgentContext。
         */
        public AgentContext withAgentPlan(
                        AgentPlan agentPlan) {

                Objects.requireNonNull(
                                agentPlan,
                                "AgentPlan must not be null");

                return toBuilder()
                                .agentPlan(agentPlan)
                                .build();
        }

        /**
         * 创建追加 Observation 后的新 AgentContext。
         *
         * <p>
         * 原 AgentContext 不会被修改。
         * </p>
         *
         * @param observation 新 Observation
         * @return 新 AgentContext
         */
        public AgentContext appendObservation(
                        ToolObservation observation) {

                Objects.requireNonNull(
                                observation,
                                "ToolObservation must not be null");

                List<ToolObservation> updatedObservations = new ArrayList<>(
                                observations);

                updatedObservations.add(
                                observation);

                return toBuilder()
                                .observations(
                                                updatedObservations)
                                .build();
        }

        /**
         * 创建一个追加日志后的新 AgentContext。
         */
        public AgentContext appendExecutionLog(
                        String executionLog) {

                String normalizedLog = normalizeExecutionLog(
                                executionLog);

                List<String> updatedLogs = new ArrayList<>(
                                executionLogs);

                updatedLogs.add(
                                normalizedLog);

                return toBuilder()
                                .executionLogs(
                                                updatedLogs)
                                .build();
        }

        public AgentContext appendRuntimeReasonObservation(
                        RuntimeReasonObservation observation) {

                Objects.requireNonNull(
                                observation,
                                "RuntimeReasonObservation must not be null");

                List<RuntimeReasonObservation> updated = new ArrayList<>(
                                runtimeReasonObservations);

                updated.add(
                                observation);

                return toBuilder()
                                .runtimeReasonObservations(
                                                updated)
                                .build();
        }

        private static String normalizeGoal(
                        String goal) {

                Objects.requireNonNull(
                                goal,
                                "Goal must not be null");

                String normalizedGoal = goal.trim();

                if (normalizedGoal.isEmpty()) {
                        throw new IllegalArgumentException(
                                        "Goal must not be blank");
                }

                return normalizedGoal;
        }

        /**
         * 规范化 Observation 集合。
         *
         * <p>
         * null 转换为空集合；
         * List.copyOf 创建不可修改快照，
         * 同时拒绝集合中的 null 元素。
         * </p>
         */
        private static List<ToolObservation> normalizeObservations(
                        List<ToolObservation> observations) {

                if (observations == null
                                || observations.isEmpty()) {

                        return List.of();
                }

                return List.copyOf(
                                observations);
        }

        private static List<String> normalizeExecutionLogs(
                        List<String> executionLogs) {

                if (executionLogs == null
                                || executionLogs.isEmpty()) {

                        return List.of();
                }

                List<String> normalizedLogs = executionLogs.stream()
                                .map(
                                                AgentContext::normalizeExecutionLog)
                                .toList();

                return List.copyOf(
                                normalizedLogs);
        }

        private static String normalizeExecutionLog(
                        String executionLog) {

                Objects.requireNonNull(
                                executionLog,
                                "Execution log must not be null");

                String normalizedLog = executionLog.trim();

                if (normalizedLog.isEmpty()) {
                        throw new IllegalArgumentException(
                                        "Execution log must not be blank");
                }

                return normalizedLog;
        }

        private static String normalizeFinalAnswer(
                        String finalAnswer) {

                if (finalAnswer == null) {
                        return null;
                }

                String normalized = finalAnswer.trim();

                if (normalized.isEmpty()) {
                        throw new IllegalArgumentException(
                                        "Final answer must not be blank");
                }

                return normalized;
        }

        public boolean hasFinalAnswer() {
                return finalAnswer != null;
        }

        public AgentContext withFinalAnswer(
                        String finalAnswer) {

                Objects.requireNonNull(
                                finalAnswer,
                                "Final answer must not be null");

                return toBuilder()
                                .finalAnswer(finalAnswer)
                                .build();
        }

        public boolean hasRuntimeReasonObservations() {
                return !runtimeReasonObservations.isEmpty();
        }

        public int runtimeReasonObservationCount() {
                return runtimeReasonObservations.size();
        }

        private static List<RuntimeReasonObservation> normalizeRuntimeReasonObservations(
                        List<RuntimeReasonObservation> observations) {

                if (observations == null
                                || observations.isEmpty()) {

                        return List.of();
                }

                return List.copyOf(
                                observations);
        }

        public Optional<LegalSecurityContext> getLegalSecurityContext() {

                return Optional.ofNullable(
                                legalSecurityContext);
        }

        public boolean hasLegalSecurityContext() {

                return legalSecurityContext != null;
        }

        public AgentContext withLegalSecurityContext(
                        LegalSecurityContext legalSecurityContext) {

                Objects.requireNonNull(
                                legalSecurityContext,
                                "LegalSecurityContext must not be null");

                return toBuilder()
                                .legalSecurityContext(
                                                legalSecurityContext)
                                .build();
        }
}
