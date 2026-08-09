package com.quince.lawyeraiassistant.agent.operator;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import org.springframework.core.annotation.Order;

/**
 * Dummy Planning Operator。
 *
 * <p>
 * 第一版仅用于验证 Pipeline，
 * 后续将替换为真正的 Planner。
 * </p>
 */
//@Component
@Order(300)
public class DummyPlanningOperator
        implements AgentOperator {

    @Override
    public AgentContext execute(
            AgentContext context) {

        return context.appendExecutionLog(
                "Planning completed");
    }
}