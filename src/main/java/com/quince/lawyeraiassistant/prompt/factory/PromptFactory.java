package com.quince.lawyeraiassistant.prompt.factory;

import com.quince.lawyeraiassistant.prompt.model.PromptFragment;

/**
 * 面向业务层的 Prompt 获取入口。
 *
 * <p>
 * 业务代码不应该直接依赖 PromptRegistry，
 * 而应该通过 PromptFactory 获取所需 Prompt。
 * </p>
 */
public interface PromptFactory {

    /**
     * 根据 Prompt 的逻辑名称获取 PromptFragment。
     *
     * @param name Prompt 逻辑名称
     * @return 已注册的 PromptFragment
     */
    PromptFragment get(String name);

    /**
     * 获取律师助手基础系统 Prompt。
     *
     * @return 律师系统 Prompt
     */
    PromptFragment lawyerSystem();

    /**
     * 获取 Agent Reason Prompt。
     *
     * @return Agent Reason Prompt
     */
    PromptFragment agentReason();

    /**
     * 获取 Agent Planning Prompt。
     *
     * @return Agent Planning Prompt
     */
    PromptFragment agentPlanning();

    /**
     * 获取 Agent Final Answer Prompt。
     *
     * @return Agent Final Answer Prompt
     */
    PromptFragment agentFinalAnswer();

    /**
     * 获取 Agent Reflection Prompt。
     *
     * @return Agent Reflection Prompt
     */
    PromptFragment agentReflection();

    /**
     * 获取 Agent Replanning Prompt。
     *
     * @return Agent Replanning Prompt
     */
    PromptFragment agentReplanning();

    /**
     * 获取 Agent Runtime Reason Prompt。
     *
     * @return Runtime Reason Prompt
     */
    PromptFragment agentRuntimeReason();
}