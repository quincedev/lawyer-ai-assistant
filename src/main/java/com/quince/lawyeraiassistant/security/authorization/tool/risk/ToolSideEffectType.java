package com.quince.lawyeraiassistant.security.authorization.tool.risk;

/**
 * Describes the type of side effect produced
 * by executing a Tool.
 */
public enum ToolSideEffectType {

    /**
     * Reads data without modifying application state.
     */
    READ_ONLY,

    /**
     * Changes internal application state.
     */
    WRITE,

    /**
     * Causes an effect outside the application boundary.
     *
     * Example:
     * sending email,
     * calling an external business operation.
     */
    EXTERNAL_SIDE_EFFECT,

    /**
     * Deletes or irreversibly changes important data.
     */
    DESTRUCTIVE
}