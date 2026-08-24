package com.quince.lawyeraiassistant.agent.runtime.observability;

import com.quince.lawyeraiassistant.agent.model.AgentContext;

public interface AgentExecutionObserver {

    AgentExecutionObservation start();

    void recordRetry();

    void recordNoProgressSuppression();

    void complete(
            AgentExecutionObservation observation,
            AgentContext initialContext,
            AgentContext result,
            AgentExecutionStatus executionStatus);
}