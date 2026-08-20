package com.quince.lawyeraiassistant.agent.runtime;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.stream.AgentStreamPublisher;

/**
 * Agent Runtime。
 *
 * <p>
 * 负责控制一次 Agent 请求的完整生命周期。
 * </p>
 *
 * <p>
 * 与 AgentOperator 不同，Runtime 可以控制多轮执行，
 * 包括：
 * </p>
 *
 * <ul>
 * <li>初始化 Reason / Planning</li>
 * <li>执行多个 Agent Task</li>
 * <li>控制最大执行步数</li>
 * <li>判断 Agent 是否完成</li>
 * </ul>
 */
public interface AgentRuntime {

    /**
     * 执行 Agent。
     *
     * @param context 初始 AgentContext
     * @return 最终 AgentContext
     */
    AgentContext run(AgentContext context);

    AgentContext run(
            AgentContext context,
            AgentStreamPublisher publisher);
}