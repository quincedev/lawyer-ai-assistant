package com.quince.lawyeraiassistant.security.guardrail;

/**
 * Represents the decision made by a guardrail.
 */
public enum GuardrailDecision {

    /**
     * The request is allowed to continue.
     */
    ALLOW,

    /**
     * The request must be blocked.
     */
    BLOCK
}