package com.quince.lawyeraiassistant.agent.model;

/**
 * Agent Runtime 支持的 Action 类型。
 *
 * <p>
 * Agent 在处理当前 AgentTask 时，
 * 下一步动作不一定是调用 Tool。
 * </p>
 *
 * <p>
 * 当前第一版支持：
 * </p>
 *
 * <ul>
 * <li>TOOL：调用外部能力</li>
 * <li>REASON：基于当前 Context 进行内部分析</li>
 * <li>FINAL_ANSWER：生成最终面向用户的答案</li>
 * </ul>
 */
public enum AgentActionType {

    /**
     * 调用外部 Tool。
     */
    TOOL,

    /**
     * 基于当前 AgentContext、
     * 已有 Observation 等信息进行内部分析。
     */
    REASON,

    /**
     * 基于当前执行结果生成最终用户答案。
     */
    FINAL_ANSWER
}