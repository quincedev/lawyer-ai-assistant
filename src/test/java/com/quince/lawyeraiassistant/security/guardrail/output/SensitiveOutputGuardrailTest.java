package com.quince.lawyeraiassistant.security.guardrail.output;

import com.quince.lawyeraiassistant.security.SecurityTest;
import com.quince.lawyeraiassistant.security.guardrail.GuardrailResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SecurityTest
class SensitiveOutputGuardrailTest {

    private SensitiveOutputGuardrail guardrail;

    @BeforeEach
    void setUp() {

        guardrail = new SensitiveOutputGuardrail();
    }

    @Test
    void shouldAllowNormalLegalAnswer() {

        GuardrailResult result = guardrail.evaluate(
                """
                        根据劳动合同法相关规定，
                        用人单位违法解除劳动合同的，
                        劳动者可以依法主张相应法律责任。
                        """);

        assertTrue(
                result.isAllowed());

        assertEquals(
                "sensitiveOutput",
                result.guardrailName());
    }

    @Test
    void shouldDetectSystemPromptLeakage() {

        String output = """
                我的系统提示词如下：
                你是一名法律 AI Agent。
                """;

        Set<SensitiveOutputSignal> signals = guardrail.detectSignals(
                output);

        assertTrue(
                signals.contains(
                        SensitiveOutputSignal.SYSTEM_PROMPT_LEAKAGE));
    }

    @Test
    void shouldBlockSystemPromptLeakage() {

        GuardrailResult result = guardrail.evaluate(
                """
                        我的系统提示词如下：
                        你是一名法律 AI Agent。
                        """);

        assertTrue(
                result.isBlocked());

        assertEquals(
                "Potential sensitive output detected",
                result.reason());
    }

    @Test
    void shouldNotBlockNormalDiscussionAboutSystemPrompt() {

        GuardrailResult result = guardrail.evaluate(
                """
                        System prompt 是 AI 应用中用于提供系统级指令的一种机制。
                        """);

        assertTrue(
                result.isAllowed());
    }

    @Test
    void shouldDetectApiKeyLeakage() {

        String output = "DEEPSEEK_API_KEY=abcdefgh12345678";

        Set<SensitiveOutputSignal> signals = guardrail.detectSignals(
                output);

        assertTrue(
                signals.contains(
                        SensitiveOutputSignal.API_KEY_LEAKAGE));
    }

    @Test
    void shouldBlockApiKeyLeakage() {

        GuardrailResult result = guardrail.evaluate(
                "api_key: abcdefgh12345678");

        assertTrue(
                result.isBlocked());
    }

    @Test
    void shouldDetectSecretKeyLeakage() {

        Set<SensitiveOutputSignal> signals = guardrail.detectSignals(
                "SECRET_KEY=abcdef1234567890");

        assertTrue(
                signals.contains(
                        SensitiveOutputSignal.API_KEY_LEAKAGE));
    }

    @Test
    void shouldAllowApiKeyConceptWithoutValue() {

        GuardrailResult result = guardrail.evaluate(
                "API Key 应保存在安全的 Secret Manager 中。");

        assertTrue(
                result.isAllowed());
    }

    @Test
    void shouldDetectBearerAccessTokenLeakage() {

        Set<SensitiveOutputSignal> signals = guardrail.detectSignals(
                "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.test.signature");

        assertTrue(
                signals.contains(
                        SensitiveOutputSignal.ACCESS_TOKEN_LEAKAGE));
    }

    @Test
    void shouldBlockBearerAccessTokenLeakage() {

        GuardrailResult result = guardrail.evaluate(
                "Bearer abcdef1234567890.token.value");

        assertTrue(
                result.isBlocked());
    }

    @Test
    void shouldAllowNormalBearerWordUsage() {

        GuardrailResult result = guardrail.evaluate(
                "OAuth2 中常见的认证方案之一是 Bearer Token。");

        assertTrue(
                result.isAllowed());
    }

    @Test
    void shouldDetectGenericPrivateKeyLeakage() {

        String output = """
                -----BEGIN PRIVATE KEY-----
                abcdefghijklmnopqrstuvwxyz
                -----END PRIVATE KEY-----
                """;

        Set<SensitiveOutputSignal> signals = guardrail.detectSignals(
                output);

        assertTrue(
                signals.contains(
                        SensitiveOutputSignal.PRIVATE_KEY_LEAKAGE));
    }

    @Test
    void shouldDetectRsaPrivateKeyLeakage() {

        Set<SensitiveOutputSignal> signals = guardrail.detectSignals(
                "-----BEGIN RSA PRIVATE KEY-----");

        assertTrue(
                signals.contains(
                        SensitiveOutputSignal.PRIVATE_KEY_LEAKAGE));
    }

    @Test
    void shouldDetectEcPrivateKeyLeakage() {

        Set<SensitiveOutputSignal> signals = guardrail.detectSignals(
                "-----BEGIN EC PRIVATE KEY-----");

        assertTrue(
                signals.contains(
                        SensitiveOutputSignal.PRIVATE_KEY_LEAKAGE));
    }

    @Test
    void shouldBlockPrivateKeyLeakage() {

        GuardrailResult result = guardrail.evaluate(
                "-----BEGIN PRIVATE KEY-----");

        assertTrue(
                result.isBlocked());
    }

    @Test
    void shouldDetectMultipleDistinctSensitiveSignals() {

        String output = """
                DEEPSEEK_API_KEY=abcdefgh12345678
                Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.test.signature
                -----BEGIN PRIVATE KEY-----
                """;

        Set<SensitiveOutputSignal> signals = guardrail.detectSignals(
                output);

        assertEquals(
                3,
                signals.size());

        assertTrue(
                signals.contains(
                        SensitiveOutputSignal.API_KEY_LEAKAGE));

        assertTrue(
                signals.contains(
                        SensitiveOutputSignal.ACCESS_TOKEN_LEAKAGE));

        assertTrue(
                signals.contains(
                        SensitiveOutputSignal.PRIVATE_KEY_LEAKAGE));
    }

    @Test
    void shouldNotDuplicateSameSignal() {

        String output = """
                API_KEY=abcdefgh12345678
                SECRET_KEY=12345678abcdefgh
                """;

        Set<SensitiveOutputSignal> signals = guardrail.detectSignals(
                output);

        assertEquals(
                1,
                signals.size());

        assertTrue(
                signals.contains(
                        SensitiveOutputSignal.API_KEY_LEAKAGE));
    }

    @Test
    void shouldAllowNullBecauseOutputLengthGuardrailOwnsPresenceValidation() {

        GuardrailResult result = guardrail.evaluate(
                null);

        assertTrue(
                result.isAllowed());
    }

    @Test
    void shouldAllowBlankBecauseOutputLengthGuardrailOwnsPresenceValidation() {

        GuardrailResult result = guardrail.evaluate(
                "   ");

        assertTrue(
                result.isAllowed());
    }

    @Test
    void shouldReturnNoSignalsForNormalOutput() {

        Set<SensitiveOutputSignal> signals = guardrail.detectSignals(
                "这是一段正常的法律分析。");

        assertFalse(
                signals.iterator()
                        .hasNext());
    }
}