package com.quince.lawyeraiassistant.agent.tool;

import com.quince.lawyeraiassistant.agent.model.ToolAction;
import com.quince.lawyeraiassistant.agent.model.ToolObservation;

/**
 * ToolAction 的底层执行器。
 *
 * <p>
 * 职责：
 * ToolAction
 * ↓
 * AgentToolRegistry
 * ↓
 * AgentTool
 * ↓
 * ToolExecutionResult
 * ↓
 * ToolObservation
 * </p>
 *
 * <p>
 * 注意：
 * 该组件不负责：
 * </p>
 *
 * <ul>
 * <li>选择 AgentTask</li>
 * <li>选择 AgentAction</li>
 * <li>修改 AgentPlan 状态</li>
 * <li>修改 AgentContext</li>
 * </ul>
 */
public interface ToolActionExecutor {

    ToolObservation execute(
            ToolAction action);
}