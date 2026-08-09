package com.quince.lawyeraiassistant.agent.model;

/**
 * Reflection LLM Structured Output。
 *
 * <p>
 * 该对象只负责接收模型输出，
 * Runtime Domain 最终仍然使用 ReflectionResult。
 * </p>
 *
 * @param decision Reflection Decision
 * @param summary  Reflection 摘要
 */
public record ReflectionDecisionResponse(
        ReflectionDecision decision,
        String summary) {
}