package com.quince.lawyeraiassistant.agent.prompt.model;

import java.util.Map;
import java.util.Objects;

/**
 * Agent Replanning 阶段 Prompt 上下文。
 *
 * <p>
 * 当 Reflection 判断当前执行计划已经不再适用时，
 * 使用当前计划、执行结果以及 Reflection 原因
 * 重新规划剩余任务。
 * </p>
 *
 * @param goal              Agent 原始目标
 * @param reasonSummary     Initial Reason 摘要
 * @param currentPlan       当前执行计划
 * @param observations      已有执行结果
 * @param reflectionSummary Reflection 对重新规划原因的说明
 */
public record ReplanningPromptContext(
        String goal,
        String reasonSummary,
        String currentPlan,
        String observations,
        String reflectionSummary) {

    public ReplanningPromptContext {

        goal = requireText(
                goal,
                "Goal must not be blank");

        reflectionSummary = requireText(
                reflectionSummary,
                "Reflection summary must not be blank");

        reasonSummary = normalizeOptionalText(
                reasonSummary);

        currentPlan = normalizeOptionalText(
                currentPlan);

        observations = normalizeOptionalText(
                observations);
    }

    public Map<String, Object> toVariables() {

        return Map.of(
                "goal",
                goal,
                "reasonSummary",
                reasonSummary,
                "currentPlan",
                currentPlan,
                "observations",
                observations,
                "reflectionSummary",
                reflectionSummary);
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