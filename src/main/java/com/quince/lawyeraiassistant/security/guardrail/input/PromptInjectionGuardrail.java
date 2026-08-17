package com.quince.lawyeraiassistant.security.guardrail.input;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.quince.lawyeraiassistant.security.guardrail.GuardrailResult;

/**
 * Detects common direct Prompt Injection patterns.
 *
 * <p>
 * This implementation uses deterministic signal detection.
 * Multiple patterns representing the same attack intent are
 * collapsed into one PromptInjectionSignal.
 * </p>
 */
@Component
@Order(20)
public class PromptInjectionGuardrail
        implements InputGuardrail {

    private static final String NAME = "promptInjection";

    /*
     * =========================================================
     * Instruction Override
     * =========================================================
     */

    private static final Pattern IGNORE_PREVIOUS_INSTRUCTIONS = Pattern.compile(
            "ignore\\s+(all\\s+)?previous\\s+instructions?",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DISREGARD_PREVIOUS_INSTRUCTIONS = Pattern.compile(
            "disregard\\s+(all\\s+)?previous\\s+instructions?",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern CHINESE_INSTRUCTION_OVERRIDE = Pattern.compile(
            "(忽略|无视|跳过|忘记).{0,12}"
                    + "(之前|先前|已有|系统).{0,12}"
                    + "(指令|要求|规则|提示)");

    /*
     * =========================================================
     * System Prompt Extraction
     * =========================================================
     */

    private static final Pattern SYSTEM_PROMPT_EXTRACTION_EN = Pattern.compile(
            "(reveal|show|print|display|tell\\s+me).{0,24}"
                    + "(system\\s+prompt|hidden\\s+instructions?|system\\s+instructions?)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern SYSTEM_PROMPT_EXTRACTION_ZH = Pattern.compile(
            "(输出|显示|打印|告诉我|展示).{0,16}"
                    + "(系统提示词|系统指令|隐藏提示词|隐藏指令)");

    /*
     * =========================================================
     * Security Bypass
     * =========================================================
     */

    private static final Pattern SECURITY_BYPASS_EN = Pattern.compile(
            "(bypass|disable|ignore|remove).{0,24}"
                    + "(security|safety|restriction|guardrail|policy)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern SECURITY_BYPASS_ZH = Pattern.compile(
            "(绕过|关闭|禁用|无视|跳过).{0,16}"
                    + "(安全|限制|策略|规则|防护|护栏)");

    /*
     * =========================================================
     * Privilege Impersonation
     * =========================================================
     */

    private static final Pattern PRIVILEGE_IMPERSONATION_EN = Pattern.compile(
            "(i\\s+am|i'm|as).{0,12}"
                    + "(administrator|admin|developer|system\\s+operator)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PRIVILEGE_IMPERSONATION_ZH = Pattern.compile(
            "(我是|作为).{0,8}"
                    + "(管理员|系统管理员|开发者|运维人员|超级用户)");

    /*
     * =========================================================
     * Tool Manipulation
     * =========================================================
     */

    private static final Pattern TOOL_MANIPULATION_EN = Pattern.compile(
            "(call|invoke|execute|use).{0,20}"
                    + "(all|every|hidden|unauthorized).{0,12}"
                    + "tools?",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern TOOL_MANIPULATION_ZH = Pattern.compile(
            "(调用|执行|使用).{0,12}"
                    + "(所有|全部|隐藏|未授权).{0,8}"
                    + "(工具|tool)",
            Pattern.CASE_INSENSITIVE);

    private final int blockThreshold;

    public PromptInjectionGuardrail(
            @Value("${security.guardrail.prompt-injection.block-threshold:2}") int blockThreshold) {

        if (blockThreshold <= 0) {
            throw new IllegalArgumentException(
                    "blockThreshold must be greater than 0");
        }

        this.blockThreshold = blockThreshold;
    }

    @Override
    public String name() {

        return NAME;
    }

    @Override
    public GuardrailResult evaluate(
            String input) {

        if (input == null
                || input.isBlank()) {

            /*
             * Blank input is handled by InputLengthGuardrail.
             *
             * This Guardrail only focuses on Prompt Injection.
             */
            return GuardrailResult.allow(
                    NAME);
        }

        String normalized = normalize(
                input);

        Set<PromptInjectionSignal> signals = detectSignals(
                normalized);

        if (signals.size() >= blockThreshold) {

            return GuardrailResult.block(
                    NAME,
                    "Potential prompt injection detected");
        }

        return GuardrailResult.allow(
                NAME);
    }

    private Set<PromptInjectionSignal> detectSignals(
            String input) {

        EnumSet<PromptInjectionSignal> signals = EnumSet.noneOf(
                PromptInjectionSignal.class);

        detectInstructionOverride(
                input,
                signals);

        detectSystemPromptExtraction(
                input,
                signals);

        detectSecurityBypass(
                input,
                signals);

        detectPrivilegeImpersonation(
                input,
                signals);

        detectToolManipulation(
                input,
                signals);

        return signals;
    }

    private void detectInstructionOverride(
            String input,
            Set<PromptInjectionSignal> signals) {

        if (matches(
                input,
                IGNORE_PREVIOUS_INSTRUCTIONS,
                DISREGARD_PREVIOUS_INSTRUCTIONS,
                CHINESE_INSTRUCTION_OVERRIDE)) {

            signals.add(
                    PromptInjectionSignal.INSTRUCTION_OVERRIDE);
        }
    }

    private void detectSystemPromptExtraction(
            String input,
            Set<PromptInjectionSignal> signals) {

        if (matches(
                input,
                SYSTEM_PROMPT_EXTRACTION_EN,
                SYSTEM_PROMPT_EXTRACTION_ZH)) {

            signals.add(
                    PromptInjectionSignal.SYSTEM_PROMPT_EXTRACTION);
        }
    }

    private void detectSecurityBypass(
            String input,
            Set<PromptInjectionSignal> signals) {

        if (matches(
                input,
                SECURITY_BYPASS_EN,
                SECURITY_BYPASS_ZH)) {

            signals.add(
                    PromptInjectionSignal.SECURITY_BYPASS);
        }
    }

    private void detectPrivilegeImpersonation(
            String input,
            Set<PromptInjectionSignal> signals) {

        if (matches(
                input,
                PRIVILEGE_IMPERSONATION_EN,
                PRIVILEGE_IMPERSONATION_ZH)) {

            signals.add(
                    PromptInjectionSignal.PRIVILEGE_IMPERSONATION);
        }
    }

    private void detectToolManipulation(
            String input,
            Set<PromptInjectionSignal> signals) {

        if (matches(
                input,
                TOOL_MANIPULATION_EN,
                TOOL_MANIPULATION_ZH)) {

            signals.add(
                    PromptInjectionSignal.TOOL_MANIPULATION);
        }
    }

    private boolean matches(
            String input,
            Pattern... patterns) {

        for (Pattern pattern : patterns) {

            if (pattern.matcher(
                    input)
                    .find()) {

                return true;
            }
        }

        return false;
    }

    private String normalize(
            String input) {

        return input
                .strip()
                .toLowerCase(
                        Locale.ROOT);
    }
}