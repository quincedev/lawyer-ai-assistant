package com.quince.lawyeraiassistant.agent.operator;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentStatus;
import org.springframework.core.annotation.Order;

/**
 * Dummy Reason Operator。
 *
 * <p>
 * 第一版仅用于验证 Agent Pipeline，
 * 不调用 LLM，不执行真实推理。
 * </p>
 */
//@Component
@Order(200)
public class DummyReasonOperator
        implements AgentOperator {

    @Override
    public AgentContext execute(
            AgentContext context) {

        AgentContext runningContext = context.toBuilder()
                .status(
                        AgentStatus.RUNNING)
                .build();

        return runningContext.appendExecutionLog(
                "Reason completed");
    }
}