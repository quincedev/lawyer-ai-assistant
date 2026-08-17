package com.quince.lawyeraiassistant.security.runtime;

/**
 * Runtime operation that is about to consume Agent resources.
 */
public enum RuntimeGuardrailOperation {

    STEP,

    TOOL_CALL,

    REPLAN,

    RETRY
}