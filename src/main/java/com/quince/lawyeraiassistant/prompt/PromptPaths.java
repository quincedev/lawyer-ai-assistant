package com.quince.lawyeraiassistant.prompt;

/**
 * Prompt 资源路径常量。
 *
 * <p>
 * 所有正式 Prompt 统一放在：
 * </p>
 *
 * <pre>
 * src / main / resources / prompts
 * </pre>
 *
 * <p>
 * 业务代码不应该直接依赖这些路径，
 * 路径主要由 PromptDefinition 和 PromptLoader 使用。
 * </p>
 */
public final class PromptPaths {

    /**
     * 正式律师系统 Prompt。
     */
    public static final String LAWYER_SYSTEM = "classpath:prompts/system/lawyer-system.st";

    /**
     * 案件分析教学或 Playground Prompt。
     *
     * <p>
     * 当前不一定注册到正式 PromptRegistry。
     * </p>
     */
    public static final String CASE_ANALYSIS = "classpath:prompts/legal/case-analysis.st";

    /**
     * Agent Reason Prompt。
     */
    public static final String AGENT_REASON = "classpath:prompts/agent/reason.st";

    /**
     * Agent Planning Prompt。
     */
    public static final String AGENT_PLANNING = "classpath:prompts/agent/planning.st";

    /**
     * Agent Final Answer Prompt。
     */
    public static final String AGENT_FINAL_ANSWER = "classpath:prompts/agent/final-answer.st";

    /**
     * Agent Reflection Prompt。
     */
    public static final String AGENT_REFLECTION = "classpath:prompts/agent/reflection.st";

    /**
     * Agent Replanning Prompt。
     */
    public static final String AGENT_REPLANNING = "classpath:prompts/agent/replanning.st";

    /**
     * Agent Runtime Reason Prompt。
     */
    public static final String AGENT_RUNTIME_REASON = "classpath:prompts/agent/runtime-reason.st";

    private PromptPaths() {
        throw new IllegalStateException(
                "PromptPaths is a constants class and cannot be instantiated");
    }
}