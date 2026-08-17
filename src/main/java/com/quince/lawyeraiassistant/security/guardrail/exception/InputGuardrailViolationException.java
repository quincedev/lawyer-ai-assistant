package com.quince.lawyeraiassistant.security.guardrail.exception;

import java.util.Objects;

import org.springframework.http.HttpStatus;

import com.quince.lawyeraiassistant.common.exception.BusinessException;
import com.quince.lawyeraiassistant.common.exception.ErrorCode;

/**
 * Thrown when external input is blocked by an Input Guardrail.
 *
 * Internal security details are available through:
 *
 * guardrailName
 *
 * External API clients receive a stable and safe error message.
 */
public class InputGuardrailViolationException
        extends BusinessException {

    private static final String SAFE_MESSAGE = "请求内容未通过安全检查";

    private final String guardrailName;

    public InputGuardrailViolationException(
            String guardrailName) {

        super(
                ErrorCode.AI_INPUT_REJECTED,
                SAFE_MESSAGE,
                HttpStatus.BAD_REQUEST);

        this.guardrailName = Objects.requireNonNull(
                guardrailName,
                "guardrailName must not be null");
    }

    public String getGuardrailName() {

        return guardrailName;
    }
}