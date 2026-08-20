package com.quince.lawyeraiassistant.agent.runtime.performance.regression;

import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceSnapshot;

public interface AgentPerformanceRegressionEvaluator {

    AgentPerformanceRegressionReport evaluate(
            AgentPerformanceSnapshot snapshot,
            AgentPerformanceBaseline baseline);
}