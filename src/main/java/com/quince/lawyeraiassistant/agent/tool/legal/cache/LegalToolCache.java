package com.quince.lawyeraiassistant.agent.tool.legal.cache;

import java.util.Optional;

import com.quince.lawyeraiassistant.agent.model.ToolAction;
import com.quince.lawyeraiassistant.agent.tool.ToolExecutionContext;

public interface LegalToolCache {

    Optional<LegalToolCacheHit> get(
            ToolExecutionContext executionContext,
            ToolAction action);

    void put(
            ToolExecutionContext executionContext,
            ToolAction action,
            String rawResult);
}