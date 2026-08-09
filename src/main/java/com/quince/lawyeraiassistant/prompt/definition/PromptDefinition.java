package com.quince.lawyeraiassistant.prompt.definition;

import com.quince.lawyeraiassistant.prompt.PromptNames;
import com.quince.lawyeraiassistant.prompt.PromptPaths;

/**
 * 项目正式 Prompt 的统一元数据定义。
 *
 * <p>
 * 每个正式 Prompt 应在此处声明：
 * </p>
 *
 * <ul>
 * <li>逻辑名称</li>
 * <li>classpath 资源位置</li>
 * <li>版本</li>
 * </ul>
 *
 * <p>
 * PromptRegistryInitializer 会遍历本枚举，
 * 自动加载并注册全部正式 Prompt。
 * </p>
 */
public enum PromptDefinition {

    /**
     * 法律问答系统 Prompt。
     */
    LAWYER_SYSTEM(
            PromptNames.LAWYER_SYSTEM,
            PromptPaths.LAWYER_SYSTEM,
            "v1"),

    /**
     * Agent Reason 阶段 Prompt。
     */
    AGENT_REASON(
            PromptNames.AGENT_REASON,
            PromptPaths.AGENT_REASON,
            "v1"),

    /**
     * Agent Planning 阶段 Prompt。
     */
    AGENT_PLANNING(
            PromptNames.AGENT_PLANNING,
            PromptPaths.AGENT_PLANNING,
            "v1"),

    /**
     * Agent Final Answer 阶段 Prompt。
     */
    AGENT_FINAL_ANSWER(
            PromptNames.AGENT_FINAL_ANSWER,
            PromptPaths.AGENT_FINAL_ANSWER,
            "v1"),

    /**
     * Agent Reflection 阶段 Prompt。
     */
    AGENT_REFLECTION(
            PromptNames.AGENT_REFLECTION,
            PromptPaths.AGENT_REFLECTION,
            "v1"),

    /**
     * Agent Replanning 阶段 Prompt。
     */
    AGENT_REPLANNING(
            PromptNames.AGENT_REPLANNING,
            PromptPaths.AGENT_REPLANNING,
            "v1"),

    /**
     * Agent Runtime Reason 阶段 Prompt。
     */
    AGENT_RUNTIME_REASON(
            PromptNames.AGENT_RUNTIME_REASON,
            PromptPaths.AGENT_RUNTIME_REASON,
            "v1");

    private final String name;

    private final String location;

    private final String version;

    PromptDefinition(
            String name,
            String location,
            String version) {

        this.name = name;
        this.location = location;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public String getVersion() {
        return version;
    }
}