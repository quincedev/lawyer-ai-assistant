package com.quince.lawyeraiassistant.security.guardrail.output;

import com.quince.lawyeraiassistant.security.guardrail.GuardrailResult;

/**
 * Output Guardrail SPI.
 *
 * <p>
 * 用于在 Agent / LLM 输出离开应用边界之前，
 * 对最终输出执行安全评估。
 * </p>
 *
 * <p>
 * 每个 OutputGuardrail 只负责一种明确的输出安全策略，
 * 例如：
 * </p>
 *
 * <pre>
 * OutputLengthGuardrail
 * SensitiveOutputGuardrail
 * </pre>
 */
public interface OutputGuardrail {

    /**
     * Guardrail 的唯一名称。
     */
    String name();

    /**
     * 对 Agent 最终输出进行安全评估。
     *
     * @param output Agent / LLM 最终输出
     * @return GuardrailResult
     */
    GuardrailResult evaluate(
            String output);
}