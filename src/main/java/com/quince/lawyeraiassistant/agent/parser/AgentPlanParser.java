package com.quince.lawyeraiassistant.agent.parser;

import com.quince.lawyeraiassistant.agent.model.AgentPlan;
import com.quince.lawyeraiassistant.agent.model.AgentTask;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * AgentPlan 文本解析器。
 *
 * <p>
 * 负责将 LLM 返回的 Planning / Replanning 文本
 * 转换为结构化 AgentPlan。
 * </p>
 *
 * <p>
 * 当前支持的格式：
 * </p>
 *
 * <pre>
 * task-1|读取劳动合同
 * task-2|识别法律风险
 * task-3|生成律师意见书
 * </pre>
 */
@Component
public class AgentPlanParser {

    private static final String TASK_SEPARATOR = "\\|";

    /**
     * 将 LLM 返回的计划文本解析为 AgentPlan。
     *
     * @param content Planning / Replanning 文本
     * @return AgentPlan
     */
    public AgentPlan parse(
            String content) {

        Objects.requireNonNull(
                content,
                "Agent plan content must not be null");

        if (content.isBlank()) {
            throw new IllegalArgumentException(
                    "Agent plan content must not be blank");
        }

        String[] lines = content.strip()
                .split("\\R");

        List<AgentTask> tasks = new ArrayList<>();

        for (String line : lines) {

            String normalizedLine = line.trim();

            if (normalizedLine.isEmpty()) {
                continue;
            }

            tasks.add(
                    parseTask(
                            normalizedLine));
        }

        if (tasks.isEmpty()) {
            throw new IllegalStateException(
                    "Agent plan must contain at least one task");
        }

        try {
            return AgentPlan.from(
                    tasks);
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "Invalid agent plan",
                    exception);
        }
    }

    private AgentTask parseTask(
            String line) {

        String[] parts = line.split(
                TASK_SEPARATOR,
                2);

        if (parts.length != 2) {
            throw new IllegalStateException(
                    "Invalid agent plan task format: "
                            + line);
        }

        String taskId = parts[0].trim();

        String taskDescription = parts[1].trim();

        try {
            return AgentTask.pending(
                    taskId,
                    taskDescription);
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "Invalid agent plan task: "
                            + line,
                    exception);
        }
    }
}