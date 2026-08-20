package com.quince.lawyeraiassistant.security.runtime.performance;

import java.util.List;

import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceSnapshot;

public interface PerformanceGuardrailService {

    List<PerformanceGuardrailResult> evaluate(AgentPerformanceSnapshot snapshot);
}
