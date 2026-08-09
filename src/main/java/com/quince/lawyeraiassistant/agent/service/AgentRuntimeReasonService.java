package com.quince.lawyeraiassistant.agent.service;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentTask;

/**
 * Agent Runtime Reason 能力。
 *
 * <p>
 * 与 Initial AgentReasonService 不同，
 * 该 Service 用于 Agent Loop 执行过程中，
 * 基于当前任务以及已有 Observation 进行阶段性推理。
 * </p>
 */
public interface AgentRuntimeReasonService {

    String reason(
            AgentContext context,
            AgentTask task);
}