package com.quince.lawyeraiassistant.agent.service;

import com.quince.lawyeraiassistant.agent.model.AgentContext;

/**
 * Agent 最终答案生成服务。
 *
 * <p>
 * 负责基于一次 Agent 执行过程中已经获得的上下文，
 * 生成最终面向用户的回答。
 * </p>
 *
 * <p>
 * Final Answer 与 ReasonResult 不同：
 * </p>
 *
 * <ul>
 * <li>ReasonResult 用于 Agent 内部理解用户目标</li>
 * <li>Final Answer 用于最终返回给用户</li>
 * </ul>
 */
public interface AgentFinalAnswerService {

    /**
     * 生成最终用户答案。
     *
     * @param context 当前 AgentContext
     * @return 最终答案
     */
    String generate(AgentContext context);
}