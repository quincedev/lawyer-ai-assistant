package com.quince.lawyeraiassistant.agent.runtime.metrics.micrometer;

import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceSnapshot;
import com.quince.lawyeraiassistant.security.runtime.performance.PerformanceGuardrailResult;

import java.util.List;

public interface AgentMicrometerMetrics {

    void recordExecution(
            AgentPerformanceSnapshot snapshot);

    void recordGuardrailResults(
            List<PerformanceGuardrailResult> results);
}