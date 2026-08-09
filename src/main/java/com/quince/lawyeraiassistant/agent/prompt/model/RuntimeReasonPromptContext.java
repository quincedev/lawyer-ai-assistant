package com.quince.lawyeraiassistant.agent.prompt.model;

import java.util.Map;
import java.util.Objects;

/**
 * Agent Runtime Reason 阶段 Prompt 上下文。
 *
 * <p>
 * 用于 Agent Loop 执行过程中，
 * 基于当前任务、当前计划以及已有 Observation
 * 进行阶段性推理。
 * </p>
 *
 * @param goal         Agent 原始目标
 * @param currentTask  当前正在处理的任务
 * @param currentPlan  当前 Agent Plan
 * @param observations 已有执行结果
 */
public record RuntimeReasonPromptContext(
        String goal,
        String currentTask,
        String currentPlan,
        String observations) {

    public RuntimeReasonPromptContext {

        goal = requireText(
                goal,
                "Goal must not be blank");

        currentTask = requireText(
                currentTask,
                "Current task must not be blank");

        currentPlan = normalizeOptionalText(
                currentPlan);

        observations = normalizeOptionalText(
                observations);
    }

    public Map<String, Object> toVariables() {

        return Map.of(
                "goal",
                goal,
                "currentTask",
                currentTask,
                "currentPlan",
                currentPlan,
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