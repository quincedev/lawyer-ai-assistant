package com.quince.lawyeraiassistant.agent.runtime.evidence;

import com.quince.lawyeraiassistant.agent.model.AgentTask;
import com.quince.lawyeraiassistant.agent.model.ToolObservation;

public interface AgentEvidencePolicy {

    EvidenceValidationResult validate(
            AgentTask task,
            ToolObservation observation);
}