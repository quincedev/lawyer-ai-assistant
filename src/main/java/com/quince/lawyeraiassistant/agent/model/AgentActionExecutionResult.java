package com.quince.lawyeraiassistant.agent.model;

import java.util.Objects;

/**
 * AgentAction 的统一执行结果。
 *
 * <p>
 * 将 TOOL / REASON / FINAL_ANSWER
 * 不同执行结果统一包装，
 * 避免 Runtime 直接依赖具体 Action 实现。
 * </p>
 */
public final class AgentActionExecutionResult {

    private final AgentActionType actionType;

    private final ToolObservation observation;

    private final String content;

    private AgentActionExecutionResult(
            AgentActionType actionType,
            ToolObservation observation,
            String content) {

        this.actionType = Objects.requireNonNull(
                actionType,
                "AgentActionType must not be null");

        this.observation = observation;

        this.content = content;
    }

    public static AgentActionExecutionResult tool(
            ToolObservation observation) {

        return new AgentActionExecutionResult(
                AgentActionType.TOOL,
                Objects.requireNonNull(
                        observation,
                        "ToolObservation must not be null"),
                null);
    }

    public static AgentActionExecutionResult reason(
            String content) {

        return new AgentActionExecutionResult(
                AgentActionType.REASON,
                null,
                requireText(
                        content,
                        "Reason content must not be blank"));
    }

    public static AgentActionExecutionResult finalAnswer(
            String content) {

        return new AgentActionExecutionResult(
                AgentActionType.FINAL_ANSWER,
                null,
                requireText(
                        content,
                        "Final answer content must not be blank"));
    }

    public AgentActionType getActionType() {
        return actionType;
    }

    public ToolObservation getObservation() {
        return observation;
    }

    public String getContent() {
        return content;
    }

    public boolean isTool() {
        return actionType == AgentActionType.TOOL;
    }

    public boolean isReason() {
        return actionType == AgentActionType.REASON;
    }

    public boolean isFinalAnswer() {
        return actionType == AgentActionType.FINAL_ANSWER;
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
}