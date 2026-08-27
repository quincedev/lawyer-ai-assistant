package com.quince.lawyeraiassistant.agent.runtime.cost;

import java.util.Optional;

public interface AgentExecutionCostAttributionRecorder {

    void record(
            AgentExecutionCostAttribution attribution);

    Optional<AgentExecutionCostAttribution> latest();

    void clear();
}