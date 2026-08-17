package com.quince.lawyeraiassistant.security.authorization.tool.risk;

/**
 * Security risk level associated with executing a Tool.
 */
public enum ToolRiskLevel {

    /**
     * Read-only or otherwise low-impact operation.
     *
     * Example:
     * searchLegalKnowledge
     */
    LOW,

    /**
     * Operation may modify application state,
     * but the impact is normally recoverable.
     *
     * Example:
     * createDocument
     * updateDraft
     */
    MEDIUM,

    /**
     * Operation has significant external,
     * destructive or security-sensitive side effects.
     *
     * Example:
     * sendEmail
     * deleteCase
     */
    HIGH
}