package com.quince.lawyeraiassistant.security.guardrail.input;

import com.quince.lawyeraiassistant.security.guardrail.GuardrailResult;

/**
 * Input Guardrail SPI.
 *
 * <p>
 * 用于在外部输入进入 Agent Runtime 之前，
 * 对输入执行安全评估。
 * </p>
 *
 * <p>
 * 每个 InputGuardrail 只负责一种明确的安全策略，
 * 例如：
 * </p>
 *
 * <pre>
 * InputLengthGuardrail
 * PromptInjectionGuardrail
 * DomainGuardrail
 * SensitiveDataGuardrail
 * </pre>
 */
public interface InputGuardrail {

    /**
     * Guardrail 的唯一名称。
     *
     * <p>
     * 主要用于：
     * </p>
     *
     * <pre>
     * Logging
     * Security Audit
     * Observability
     * Testing
     * </pre>
     */
    String name();

    /**
     * 对输入进行安全评估。
     *
     * @param input 外部输入
     * @return GuardrailResult
     */
    GuardrailResult evaluate(
            String input);
}