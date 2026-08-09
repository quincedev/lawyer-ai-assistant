package com.quince.lawyeraiassistant.agent.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

/**
 * Agent Runtime 中统一的 Action Model。
 *
 * <p>
 * AgentAction 表示 Agent 为完成当前 AgentTask，
 * 决定采取的下一步动作。
 * </p>
 *
 * <p>
 * 当前支持：
 * </p>
 *
 * <ul>
 * <li>TOOL：调用外部 Tool</li>
 * <li>REASON：进行内部分析</li>
 * <li>FINAL_ANSWER：生成最终用户答案</li>
 * </ul>
 *
 * <p>
 * ToolAction 不会被废弃。
 * 它成为 TOOL 类型 AgentAction 的具体执行载荷。
 * </p>
 */
@Getter
@ToString
@EqualsAndHashCode
public final class AgentAction {

    /**
     * 当前 Action 对应的 AgentTask ID。
     */
    private final String taskId;

    /**
     * Action 类型。
     */
    private final AgentActionType type;

    /**
     * TOOL Action 对应的 ToolAction。
     *
     * <p>
     * 当 type = TOOL 时必须存在；
     * 当 type = REASON / FINAL_ANSWER 时必须为 null。
     * </p>
     */
    private final ToolAction toolAction;

    private AgentAction(
            String taskId,
            AgentActionType type,
            ToolAction toolAction) {

        this.taskId = normalizeTaskId(
                taskId);

        this.type = Objects.requireNonNull(
                type,
                "AgentActionType must not be null");

        this.toolAction = toolAction;

        validateState();
    }

    /**
     * 创建 TOOL Action。
     *
     * @param toolAction Tool Action
     * @return AgentAction
     */
    public static AgentAction tool(
            ToolAction toolAction) {

        Objects.requireNonNull(
                toolAction,
                "ToolAction must not be null");

        return new AgentAction(
                toolAction.getTaskId(),
                AgentActionType.TOOL,
                toolAction);
    }

    /**
     * 创建 REASON Action。
     *
     * @param taskId AgentTask ID
     * @return AgentAction
     */
    public static AgentAction reason(
            String taskId) {

        return new AgentAction(
                taskId,
                AgentActionType.REASON,
                null);
    }

    /**
     * 创建 FINAL_ANSWER Action。
     *
     * @param taskId AgentTask ID
     * @return AgentAction
     */
    public static AgentAction finalAnswer(
            String taskId) {

        return new AgentAction(
                taskId,
                AgentActionType.FINAL_ANSWER,
                null);
    }

    /**
     * 判断当前是否为 TOOL Action。
     */
    public boolean isTool() {
        return type == AgentActionType.TOOL;
    }

    /**
     * 判断当前是否为 REASON Action。
     */
    public boolean isReason() {
        return type == AgentActionType.REASON;
    }

    /**
     * 判断当前是否为 FINAL_ANSWER Action。
     */
    public boolean isFinalAnswer() {
        return type == AgentActionType.FINAL_ANSWER;
    }

    /**
     * 获取 ToolAction。
     *
     * <p>
     * 只有当前 Action 类型为 TOOL 时允许访问。
     * </p>
     *
     * @return ToolAction
     */
    public ToolAction requireToolAction() {

        if (!isTool()) {
            throw new IllegalStateException(
                    "Current AgentAction is not a TOOL action");
        }

        return toolAction;
    }

    private void validateState() {

        if (type == AgentActionType.TOOL) {

            if (toolAction == null) {
                throw new IllegalArgumentException(
                        "TOOL action must contain ToolAction");
            }

            if (!taskId.equals(
                    toolAction.getTaskId())) {

                throw new IllegalArgumentException(
                        "AgentAction taskId must match ToolAction taskId");
            }

            return;
        }

        if (toolAction != null) {
            throw new IllegalArgumentException(
                    type
                            + " action must not contain ToolAction");
        }
    }

    private static String normalizeTaskId(
            String taskId) {

        Objects.requireNonNull(
                taskId,
                "Task id must not be null");

        String normalizedTaskId = taskId.trim();

        if (normalizedTaskId.isEmpty()) {
            throw new IllegalArgumentException(
                    "Task id must not be blank");
        }

        return normalizedTaskId;
    }
}