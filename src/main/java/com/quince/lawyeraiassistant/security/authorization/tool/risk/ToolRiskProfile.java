package com.quince.lawyeraiassistant.security.authorization.tool.risk;

import java.util.Objects;

/**
 * Immutable security metadata describing a Tool's
 * execution risk and side-effect characteristics.
 *
 * @param toolName       tool name
 * @param riskLevel      execution risk level
 * @param sideEffectType side-effect classification
 */
public record ToolRiskProfile(
        String toolName,
        ToolRiskLevel riskLevel,
        ToolSideEffectType sideEffectType) {

    public ToolRiskProfile {

        Objects.requireNonNull(
                toolName,
                "toolName must not be null");

        Objects.requireNonNull(
                riskLevel,
                "riskLevel must not be null");

        Objects.requireNonNull(
                sideEffectType,
                "sideEffectType must not be null");

        toolName = toolName.strip();

        if (toolName.isEmpty()) {
            throw new IllegalArgumentException(
                    "toolName must not be blank");
        }
    }

    public static ToolRiskProfile lowReadOnly(
            String toolName) {

        return new ToolRiskProfile(
                toolName,
                ToolRiskLevel.LOW,
                ToolSideEffectType.READ_ONLY);
    }

    public boolean isHighRisk() {

        return riskLevel == ToolRiskLevel.HIGH;
    }

    public boolean hasSideEffect() {

        return sideEffectType != ToolSideEffectType.READ_ONLY;
    }
}