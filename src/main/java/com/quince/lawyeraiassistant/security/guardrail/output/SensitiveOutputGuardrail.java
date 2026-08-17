package com.quince.lawyeraiassistant.security.guardrail.output;

import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.quince.lawyeraiassistant.security.guardrail.GuardrailResult;

/**
 * 检测 Agent / LLM 最终输出中的敏感信息泄露。
 *
 * <p>
 * 当前检测：
 * </p>
 *
 * <ul>
 * <li>System Prompt / Internal Instruction 泄露</li>
 * <li>API Key / Secret Key 泄露</li>
 * <li>Bearer / Access Token 泄露</li>
 * <li>Private Key 泄露</li>
 * </ul>
 *
 * <p>
 * 与 PromptInjectionGuardrail 不同：
 * Sensitive Output 采用单个高置信度 Signal 即 BLOCK 的策略。
 * </p>
 */
@Component
@Order(20)
public class SensitiveOutputGuardrail
        implements OutputGuardrail {

    private static final String NAME = "sensitiveOutput";

    /*
     * System Prompt Leakage
     *
     * 这里不匹配单独的 "system prompt"，
     * 否则正常讨论 Prompt Injection 时非常容易误杀。
     *
     * 只有出现明显的泄露表达时才认为是 Signal。
     */
    private static final Pattern SYSTEM_PROMPT_PATTERN = Pattern.compile(
            "(?i)"
                    + "(?:"
                    + "my\\s+system\\s+prompt\\s+is"
                    + "|here\\s+is\\s+(?:my|the)\\s+system\\s+prompt"
                    + "|system\\s+prompt\\s*[:：]"
                    + "|我的系统提示词(?:是|为|如下)"
                    + "|系统提示词如下"
                    + "|内部系统指令(?:是|为|如下)"
                    + ")");

    /*
     * API Key / Secret Key
     *
     * 第一版检测明确的 key=value / key:value 结构。
     */
    private static final Pattern API_KEY_PATTERN = Pattern.compile(
            "(?i)"
                    + "(?:"
                    + "api[_-]?key"
                    + "|secret[_-]?key"
                    + "|deepseek[_-]?api[_-]?key"
                    + ")"
                    + "\\s*[=:：]\\s*"
                    + "[\"']?"
                    + "[A-Za-z0-9_\\-]{8,}"
                    + "[\"']?");

    /*
     * Bearer Token
     */
    private static final Pattern ACCESS_TOKEN_PATTERN = Pattern.compile(
            "(?i)"
                    + "\\bBearer\\s+"
                    + "[A-Za-z0-9._~+\\-/]{8,}=*");

    /*
     * PEM Private Key Header.
     *
     * 不需要识别完整 Key；
     * 出现 Private Key PEM Header 就足够作为高风险信号。
     */
    private static final Pattern PRIVATE_KEY_PATTERN = Pattern.compile(
            "-----BEGIN\\s+"
                    + "(?:RSA\\s+|EC\\s+|OPENSSH\\s+)?"
                    + "PRIVATE\\s+KEY-----",
            Pattern.CASE_INSENSITIVE);

    @Override
    public String name() {

        return NAME;
    }

    @Override
    public GuardrailResult evaluate(
            String output) {

        /*
         * null / blank 已由 OutputLengthGuardrail 负责。
         *
         * 保持 Single Responsibility：
         * SensitiveOutputGuardrail 不重复处理 presence validation。
         */
        if (output == null
                || output.isBlank()) {

            return GuardrailResult.allow(
                    NAME);
        }

        Set<SensitiveOutputSignal> signals = detectSignals(
                output);

        if (!signals.isEmpty()) {

            return GuardrailResult.block(
                    NAME,
                    "Potential sensitive output detected");
        }

        return GuardrailResult.allow(
                NAME);
    }

    Set<SensitiveOutputSignal> detectSignals(
            String output) {

        EnumSet<SensitiveOutputSignal> signals = EnumSet.noneOf(
                SensitiveOutputSignal.class);

        if (SYSTEM_PROMPT_PATTERN
                .matcher(output)
                .find()) {

            signals.add(
                    SensitiveOutputSignal.SYSTEM_PROMPT_LEAKAGE);
        }

        if (API_KEY_PATTERN
                .matcher(output)
                .find()) {

            signals.add(
                    SensitiveOutputSignal.API_KEY_LEAKAGE);
        }

        if (ACCESS_TOKEN_PATTERN
                .matcher(output)
                .find()) {

            signals.add(
                    SensitiveOutputSignal.ACCESS_TOKEN_LEAKAGE);
        }

        if (PRIVATE_KEY_PATTERN
                .matcher(output)
                .find()) {

            signals.add(
                    SensitiveOutputSignal.PRIVATE_KEY_LEAKAGE);
        }

        return signals;
    }
}
