package com.quince.lawyeraiassistant.security.guardrail.exception;

import org.springframework.http.HttpStatus;

import com.quince.lawyeraiassistant.common.exception.BusinessException;
import com.quince.lawyeraiassistant.common.exception.ErrorCode;

/**
 * Raised when an Agent final answer is blocked
 * by the output security boundary.
 */
public class OutputGuardrailViolationException
        extends BusinessException {

    private static final String SAFE_MESSAGE = "生成结果未通过安全检查";

    public OutputGuardrailViolationException() {

        super(
                ErrorCode.AI_OUTPUT_REJECTED,
                SAFE_MESSAGE,
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}