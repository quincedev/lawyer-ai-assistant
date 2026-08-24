package com.quince.lawyeraiassistant.agent.runtime.resource;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.ToolObservation;

public interface AgentContextSizeEstimator {

    int estimate(
            AgentContext context);

    int estimateObservation(
            ToolObservation observation);

    int estimateText(
            String value);
}