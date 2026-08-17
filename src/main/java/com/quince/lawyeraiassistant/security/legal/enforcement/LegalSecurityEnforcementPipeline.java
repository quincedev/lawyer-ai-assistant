package com.quince.lawyeraiassistant.security.legal.enforcement;

import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Legal AI Security Enforcement Pipeline。
 *
 * 描述一次 Legal Agent Execution 中
 * 安全边界应遵循的逻辑顺序。
 *
 * 本类不替代已有 Security Policy，
 * 也不直接执行 Guardrail。
 *
 * 真正的 enforcement 仍由：
 *
 * Input Guardrail
 * ToolAuthorizationService
 * MCP Security
 * LegalEvidenceTrustPolicy
 * Runtime Guardrail
 * Resource Guardrail
 *
 * 等现有组件负责。
 */
@Component
public final class LegalSecurityEnforcementPipeline {

    private static final List<LegalSecurityEnforcementStage> STAGES = List.of(
            LegalSecurityEnforcementStage.INPUT_GUARDRAIL,
            LegalSecurityEnforcementStage.SECURITY_CONTEXT,
            LegalSecurityEnforcementStage.SKILL_BOUNDARY,
            LegalSecurityEnforcementStage.ACTION_SELECTION,
            LegalSecurityEnforcementStage.TOOL_EXISTENCE,
            LegalSecurityEnforcementStage.TOOL_SCOPE,
            LegalSecurityEnforcementStage.TOOL_RISK,
            LegalSecurityEnforcementStage.TOOL_AUTHORIZATION,
            LegalSecurityEnforcementStage.MCP_SECURITY,
            LegalSecurityEnforcementStage.TOOL_EXECUTION,
            LegalSecurityEnforcementStage.EVIDENCE_TRUST,
            LegalSecurityEnforcementStage.RESOURCE_PROTECTION,
            LegalSecurityEnforcementStage.MODEL_VISIBLE_EVIDENCE,
            LegalSecurityEnforcementStage.FINAL_ANSWER);

    public List<LegalSecurityEnforcementStage> stages() {

        return STAGES;
    }

    public int positionOf(
            LegalSecurityEnforcementStage stage) {

        if (stage == null) {
            throw new NullPointerException(
                    "stage must not be null");
        }

        return STAGES.indexOf(
                stage);
    }

    public boolean occursBefore(
            LegalSecurityEnforcementStage first,
            LegalSecurityEnforcementStage second) {

        int firstPosition = positionOf(
                first);

        int secondPosition = positionOf(
                second);

        if (firstPosition < 0) {
            throw new IllegalArgumentException(
                    "Unknown first security stage: "
                            + first);
        }

        if (secondPosition < 0) {
            throw new IllegalArgumentException(
                    "Unknown second security stage: "
                            + second);
        }

        return firstPosition < secondPosition;
    }
}