package com.quince.lawyeraiassistant.security.guardrail.output;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.quince.lawyeraiassistant.security.guardrail.GuardrailResult;

/**
 * Output Guardrail Chain.
 *
 * <p>
 * 按顺序执行所有已注册的 OutputGuardrail。
 * 一旦某个 Guardrail 返回 BLOCK，
 * 立即停止后续 Guardrail 执行。
 * </p>
 *
 * <pre>
 * Agent Output
 *      ↓
 * Guardrail A
 *      ↓ ALLOW
 * Guardrail B
 *      ↓ BLOCK
 * STOP
 * </pre>
 */
@Component
public class OutputGuardrailChain {

    private static final String NAME = "outputGuardrailChain";

    private final List<OutputGuardrail> guardrails;

    public OutputGuardrailChain(
            List<OutputGuardrail> guardrails) {

        Objects.requireNonNull(
                guardrails,
                "guardrails must not be null");

        this.guardrails = List.copyOf(
                guardrails);
    }

    /**
     * 对 Agent 最终输出执行完整 Output Guardrail Chain。
     *
     * <p>
     * First BLOCK wins。
     * </p>
     */
    public GuardrailResult evaluate(
            String output) {

        for (OutputGuardrail guardrail : guardrails) {

            GuardrailResult result = guardrail.evaluate(
                    output);

            Objects.requireNonNull(
                    result,
                    "OutputGuardrail must not return null: "
                            + guardrail.name());

            if (result.isBlocked()) {

                return result;
            }
        }

        return GuardrailResult.allow(
                NAME);
    }
}