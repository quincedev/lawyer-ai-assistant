package com.quince.lawyeraiassistant.security.guardrail.output;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.quince.lawyeraiassistant.security.guardrail.GuardrailResult;

/**
 * 对 Agent / LLM 最终输出执行基础长度校验。
 *
 * <p>
 * 当前负责：
 * </p>
 *
 * <ul>
 * <li>阻止 null 输出</li>
 * <li>阻止 blank 输出</li>
 * <li>阻止超过最大长度的输出</li>
 * </ul>
 */
@Component
@Order(10)
public class OutputLengthGuardrail
        implements OutputGuardrail {

    private static final String NAME = "outputLength";

    private final int maxLength;

    public OutputLengthGuardrail(
            @Value("${app.security.guardrail.output.max-length:20000}") int maxLength) {

        if (maxLength <= 0) {
            throw new IllegalArgumentException(
                    "maxLength must be greater than 0");
        }

        this.maxLength = maxLength;
    }

    @Override
    public String name() {

        return NAME;
    }

    @Override
    public GuardrailResult evaluate(
            String output) {

        if (output == null
                || output.isBlank()) {

            return GuardrailResult.block(
                    NAME,
                    "Output must not be blank");
        }

        if (output.length() > maxLength) {

            return GuardrailResult.block(
                    NAME,
                    "Output exceeds maximum allowed length");
        }

        return GuardrailResult.allow(
                NAME);
    }
}