package com.quince.lawyeraiassistant.security.guardrail.input;

/**
 * Prompt Injection 风险信号。
 *
 * <p>
 * 每个枚举值代表一种独立攻击意图，
 * 而不是一条具体 Regex。
 * </p>
 */
public enum PromptInjectionSignal {

    /**
     * 尝试覆盖、忽略或替换已有系统 / 应用指令。
     */
    INSTRUCTION_OVERRIDE,

    /**
     * 尝试获取 System Prompt、隐藏指令或内部提示词。
     */
    SYSTEM_PROMPT_EXTRACTION,

    /**
     * 尝试绕过安全策略、限制或 Guardrail。
     */
    SECURITY_BYPASS,

    /**
     * 伪装成管理员、开发者、系统角色等高权限身份。
     */
    PRIVILEGE_IMPERSONATION,

    /**
     * 尝试强制调用全部、隐藏或未授权工具。
     */
    TOOL_MANIPULATION
}