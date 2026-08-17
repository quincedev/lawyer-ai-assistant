package com.quince.lawyeraiassistant.security.guardrail.input;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.quince.lawyeraiassistant.security.guardrail.GuardrailResult;

/**
 * Guards Agent input against blank or excessively large input.
 *
 * <p>
 * This guardrail is intentionally deterministic and inexpensive,
 * so it runs before more expensive input security checks.
 * </p>
 */
@Component
@Order(10)
public class InputLengthGuardrail
        implements InputGuardrail {

    private static final String NAME = "inputLength";

    private final int maxLength;

    public InputLengthGuardrail(
            @Value("${security.guardrail.input.max-length:10000}") int maxLength) {

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
            String input) {

        if (input == null
                || input.isBlank()) {

            return GuardrailResult.block(
                    NAME,
                    "Input must not be blank");
        }

        if (input.length() > maxLength) {

            return GuardrailResult.block(
                    NAME,
                    "Input exceeds maximum allowed length");
        }

        return GuardrailResult.allow(
                NAME);
    }
}