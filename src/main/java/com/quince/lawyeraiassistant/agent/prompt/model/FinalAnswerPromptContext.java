package com.quince.lawyeraiassistant.agent.prompt.model;

import java.util.Map;
import java.util.Objects;

/**
 * Agent Final Answer 阶段 Prompt 上下文。
 *
 * <p>
 * 只保存生成最终用户答案真正需要的信息，
 * 避免直接将整个 AgentContext 暴露给 Prompt。
 * </p>
 *
 * @param goal          用户原始目标
 * @param reasonSummary Reason 阶段生成的目标理解
 * @param plan          Agent 执行计划
 * @param observations  Agent 执行过程中获得的 Observation
 */
public record FinalAnswerPromptContext(
                String goal,
                String reasonSummary,
                String plan,
                String observations) {

        public FinalAnswerPromptContext {

                goal = requireText(
                                goal,
                                "Goal must not be blank");

                reasonSummary = normalizeOptionalText(
                                reasonSummary);

                plan = normalizeOptionalText(
                                plan);

                observations = normalizeOptionalText(
                                observations);
        }

        /**
         * 转换为 Prompt Template Variables。
         */
        public Map<String, Object> toVariables() {

                return Map.of(
                                "goal",
                                goal,
                                "reasonSummary",
                                reasonSummary,
                                "plan",
                                plan,
                                "observations",
                                observations);
        }

        private static String requireText(
                        String value,
                        String message) {

                Objects.requireNonNull(
                                value,
                                message);

                String normalized = value.trim();

                if (normalized.isEmpty()) {
                        throw new IllegalArgumentException(
                                        message);
                }

                return normalized;
        }

        private static String normalizeOptionalText(
                        String value) {

                if (value == null
                                || value.isBlank()) {

                        return "无";
                }

                return value.trim();
        }
}