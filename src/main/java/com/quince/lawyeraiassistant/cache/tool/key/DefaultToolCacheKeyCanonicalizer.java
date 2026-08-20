package com.quince.lawyeraiassistant.cache.tool.key;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.quince.lawyeraiassistant.agent.tool.legal.LegalToolContract;

@Component
public class DefaultToolCacheKeyCanonicalizer
        implements ToolCacheKeyCanonicalizer {

    @Override
    public Map<String, Object> canonicalize(
            String toolName,
            Map<String, Object> arguments) {

        Objects.requireNonNull(
                toolName,
                "toolName must not be null");

        Map<String, Object> safeArguments = arguments == null
                ? Map.of()
                : arguments;

        if (!LegalToolContract.SEARCH_LEGAL_KNOWLEDGE.equals(
                toolName)) {

            return safeArguments;
        }

        return canonicalizeLegalSearchArguments(
                safeArguments);
    }

    private Map<String, Object> canonicalizeLegalSearchArguments(
            Map<String, Object> arguments) {

        Object rawQuestion = arguments.get(
                LegalToolContract.LEGAL_QUESTION);

        /*
         * 缺参数时不要在 Cache Canonicalizer 中改变 Tool Validation 行为。
         * 原样返回，由 Tool 自己负责 fail-fast。
         */
        if (rawQuestion == null) {

            return arguments;
        }

        String normalizedQuestion = normalizeLegalQuestion(
                rawQuestion.toString());

        Map<String, Object> canonical = new LinkedHashMap<>(
                arguments);

        canonical.put(
                LegalToolContract.LEGAL_QUESTION,
                normalizedQuestion);

        return Map.copyOf(
                canonical);
    }

    String normalizeLegalQuestion(
            String value) {

        if (value == null) {

            return null;
        }

        String normalized = Normalizer.normalize(
                value,
                Normalizer.Form.NFKC)
                .trim()
                .toLowerCase(
                        Locale.ROOT);

        /*
         * 先消除纯格式差异：
         * 空白、换行、Tab。
         */
        normalized = normalized.replaceAll(
                "\\s+",
                "");

        /*
         * 消除 Planner 常见的执行包装词。
         *
         * 这里只去掉“怎么执行”的表达，
         * 不删除真正的法律语义。
         */
        normalized = normalized.replace(
                "使用searchlegalknowledge",
                "");

        normalized = normalized.replace(
                "调用searchlegalknowledge",
                "");

        normalized = normalized.replace(
                "通过searchlegalknowledge",
                "");

        normalized = normalized.replace(
                "利用searchlegalknowledge",
                "");

        /*
         * 去掉常见动作前缀。
         */
        normalized = normalized.replaceFirst(
                "^(检索|查询|搜索|查找)",
                "");

        /*
         * 标点不会决定法律检索 Cache Identity。
         */
        normalized = normalized.replaceAll(
                "[，。；：、！？,.!?;:]",
                "");

        return normalized;
    }
}