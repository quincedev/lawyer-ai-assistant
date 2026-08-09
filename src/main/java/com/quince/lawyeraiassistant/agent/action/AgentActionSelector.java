package com.quince.lawyeraiassistant.agent.action;

import com.quince.lawyeraiassistant.agent.model.AgentAction;
import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentTask;

/**
 * Agent Action Selection SPI。
 *
 * <p>
 * 根据当前 AgentContext 和 AgentTask，
 * 决定下一步应该执行的 ToolAction。
 * </p>
 */
public interface AgentActionSelector {

    /**
     * 为当前 Task 选择下一步 Tool Action。
     *
     * @param context 当前 Agent Context
     * @param task    当前待执行 Task
     * @return ToolAction
     */
    AgentAction select(
            AgentContext context,
            AgentTask task);
}