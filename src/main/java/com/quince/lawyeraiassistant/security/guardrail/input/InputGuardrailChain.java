package com.quince.lawyeraiassistant.security.guardrail.input;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.quince.lawyeraiassistant.security.guardrail.GuardrailResult;

/**
 * Input Guardrail Chain.
 *
 * <p>
 * 按顺序执行所有已注册的 InputGuardrail。
 * 一旦某个 Guardrail 返回 BLOCK，
 * 立即停止后续 Guardrail 执行。
 * </p>
 *
 * <pre>
 * Input
 *   ↓
 * Guardrail A
 *   ↓ ALLOW
 * Guardrail B
 *   ↓ BLOCK
 * STOP
 * </pre>
 */
@Component
public class InputGuardrailChain {

    private static final String NAME = "inputGuardrailChain";

    private final List<InputGuardrail> guardrails;

    public InputGuardrailChain(
            List<InputGuardrail> guardrails) {

        Objects.requireNonNull(
                guardrails,
                "guardrails must not be null");

        this.guardrails = List.copyOf(
                guardrails);
    }

    /**
     * 对输入执行完整 Guardrail Chain。
     *
     * <p>
     * First BLOCK wins。
     * </p>
     */
    public GuardrailResult evaluate(
            String input) {

        for (InputGuardrail guardrail : guardrails) {

            GuardrailResult result = guardrail.evaluate(
                    input);

            Objects.requireNonNull(
                    result,
                    "InputGuardrail must not return null: "
                            + guardrail.name());

            if (result.isBlocked()) {

                return result;
            }
        }

        return GuardrailResult.allow(
                NAME);
    }
}