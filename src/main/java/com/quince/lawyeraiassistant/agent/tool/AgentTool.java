package com.quince.lawyeraiassistant.agent.tool;

import com.quince.lawyeraiassistant.agent.model.ToolAction;
import com.quince.lawyeraiassistant.agent.model.ToolExecutionResult;
import com.quince.lawyeraiassistant.security.legal.SecuritySource;

/**
 * Agent Tool 的统一 SPI。
 *
 * <p>
 * 所有可以被 Agent Runtime 执行的 Tool，
 * 都需要实现该接口。
 * </p>
 *
 * <p>
 * Agent Runtime 只依赖 AgentTool，
 * 不依赖具体 Tool 实现。
 * </p>
 */
public interface AgentTool {

    /**
     * 返回 Tool 的唯一名称。
     *
     * <p>
     * Tool 名称用于 Runtime 进行 Tool 匹配。
     * </p>
     *
     * @return Tool 名称
     */
    String name();

    /**
     * 执行 Tool。
     *
     * @param action 本次 Tool Action
     * @return Tool 执行结果
     */
    ToolExecutionResult execute(
            ToolAction action);

    default SecuritySource resultSecuritySource() {

        return SecuritySource.TOOL_RESULT;
    }
}