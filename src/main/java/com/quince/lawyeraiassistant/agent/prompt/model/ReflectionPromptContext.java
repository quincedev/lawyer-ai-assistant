package com.quince.lawyeraiassistant.agent.prompt.model;

import java.util.Map;
import java.util.Objects;

/**
 * Reflection 阶段 Prompt 上下文。
 *
 * <p>
 * Reflection 用于判断当前 AgentTask 的执行结果
 * 是否已经足够完成任务。
 * </p>
 *
 * @param goal            Agent 原始目标
 * @param reasonSummary   Initial Reason 摘要
 * @param taskId          当前 Task ID
 * @param taskDescription 当前 Task 描述
 * @param plan            当前 AgentPlan
 * @param observations    当前已经获得的 Observation
 */
public record ReflectionPromptContext(
        String goal,
        String reasonSummary,
        String taskId,
        String taskDescription,
        String plan,
        String observations) {

    public ReflectionPromptContext {

        goal = requireText(
                goal,
                "Goal must not be blank");

        taskId = requireText(
                taskId,
                "Task id must not be blank");

        taskDescription = requireText(
                taskDescription,
                "Task description must not be blank");

        reasonSummary = normalizeOptionalText(
                reasonSummary);

        plan = normalizeOptionalText(
                plan);

        observations = normalizeOptionalText(
                observations);
    }

    /**
     * 转换为 Prompt Template variables。
     */
    public Map<String, Object> toVariables() {

        return Map.of(
                "goal",
                goal,
                "reasonSummary",
                reasonSummary,
                "taskId",
                taskId,
                "taskDescription",
                taskDescription,
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