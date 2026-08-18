package com.quince.lawyeraiassistant.agent.tool;

import com.quince.lawyeraiassistant.agent.model.ToolAction;
import com.quince.lawyeraiassistant.agent.model.ToolObservation;

public interface ToolActionExecutor {

    /**
     * Legacy/internal path.
     */
    ToolObservation execute(
            ToolAction action);

    /**
     * Runtime path with trusted execution identity.
     */
    default ToolObservation execute(
            ToolExecutionContext executionContext,
            ToolAction action) {

        return execute(
                action);
    }
}