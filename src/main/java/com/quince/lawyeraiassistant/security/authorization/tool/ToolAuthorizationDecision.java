package com.quince.lawyeraiassistant.security.authorization.tool;

/**
 * Final decision made by a Tool Authorization policy.
 */
public enum ToolAuthorizationDecision {

    /**
     * Tool execution is allowed to continue.
     */
    ALLOW,

    /**
     * Tool execution must not continue.
     */
    DENY
}