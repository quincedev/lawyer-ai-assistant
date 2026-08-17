package com.quince.lawyeraiassistant.security.guardrail.output;

/**
 * Agent / LLM 输出中的敏感信息泄露信号。
 */
public enum SensitiveOutputSignal {

    /**
     * System Prompt / Internal Instruction 泄露。
     */
    SYSTEM_PROMPT_LEAKAGE,

    /**
     * API Key / Secret Key 泄露。
     */
    API_KEY_LEAKAGE,

    /**
     * Bearer Token / Access Token 泄露。
     */
    ACCESS_TOKEN_LEAKAGE,

    /**
     * PEM Private Key 泄露。
     */
    PRIVATE_KEY_LEAKAGE
}