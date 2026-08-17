package com.quince.lawyeraiassistant.security.legal.enforcement;

/**
 * Legal AI Agent 安全执行阶段。
 *
 * 该枚举描述 Security Enforcement Pipeline
 * 中各安全边界的逻辑执行顺序。
 *
 * 注意：
 * 它不是 Policy，也不负责执行安全判断。
 *
 * 它的作用是：
 *
 * 1. 明确安全架构中的阶段；
 * 2. 为日志、测试和后续 Observability 提供稳定标识；
 * 3. 防止安全链随着代码演进变得不可理解。
 */
public enum LegalSecurityEnforcementStage {

    INPUT_GUARDRAIL,

    SECURITY_CONTEXT,

    SKILL_BOUNDARY,

    ACTION_SELECTION,

    TOOL_EXISTENCE,

    TOOL_SCOPE,

    TOOL_RISK,

    TOOL_AUTHORIZATION,

    MCP_SECURITY,

    TOOL_EXECUTION,

    EVIDENCE_TRUST,

    RESOURCE_PROTECTION,

    MODEL_VISIBLE_EVIDENCE,

    FINAL_ANSWER
}