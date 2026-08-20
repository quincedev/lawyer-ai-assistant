package com.quince.lawyeraiassistant.agent.service.support;

/**
 * 表示 LLM 调用本身已经完成，
 * 但返回结果无法被当前 Agent Stage 安全消费。
 *
 * <p>
 * 典型情况：
 * </p>
 *
 * <ul>
 * <li>content == null</li>
 * <li>content.isBlank()</li>
 * <li>Structured Output 转换失败</li>
 * <li>Structured DTO 缺少必要字段</li>
 * </ul>
 *
 * <p>
 * 该异常只允许进行 bounded retry，
 * 不允许无限重试。
 * </p>
 */
public class RetryableLlmResponseException
        extends IllegalStateException {

    public RetryableLlmResponseException(
            String message) {

        super(
                message);
    }

    public RetryableLlmResponseException(
            String message,
            Throwable cause) {

        super(
                message,
                cause);
    }
}