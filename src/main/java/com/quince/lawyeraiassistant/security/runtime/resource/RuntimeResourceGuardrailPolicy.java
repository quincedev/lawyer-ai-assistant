package com.quince.lawyeraiassistant.security.runtime.resource;

import com.quince.lawyeraiassistant.security.runtime.AgentExecutionLimits;

public interface RuntimeResourceGuardrailPolicy {

    RuntimeResourceResult evaluate(
            RuntimeResourceType resourceType,
            int resourceLength,
            AgentExecutionLimits limits);
}