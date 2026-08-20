package com.quince.lawyeraiassistant.agent.tool.legal.evidence;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(LegalEvidenceCompactionProperties.class)
public class DefaultLegalEvidenceCompactor
        implements LegalEvidenceCompactor {

    /*
     * MCP normalized legal result 中，
     * 每一份 RAG Document 都以“参考资料 N”开始。
     */
    private static final Pattern DOCUMENT_PATTERN = Pattern.compile(
            "(?m)^\\s*参考资料\\s*\\d+\\s*$");

    private static final String TRUNCATED_SUFFIX = System.lineSeparator()
            + "[Evidence truncated]";

    private final LegalEvidenceCompactionProperties properties;

    public DefaultLegalEvidenceCompactor(
            LegalEvidenceCompactionProperties properties) {

        this.properties = Objects.requireNonNull(
                properties,
                "LegalEvidenceCompactionProperties must not be null");
    }

    @Override
    public String compact(
            String content) {

        if (content == null
                || content.isBlank()) {

            return content;
        }

        if (!properties.isEnabled()) {

            return content;
        }

        String normalized = normalize(
                content);

        if (normalized.length() <= properties.getMaxChars()) {

            return normalized;
        }

        /*
         * 不能再按空行切 section。
         *
         * MCP Result 本身一个 Document 内部就有：
         *
         * 参考资料
         * 来源
         * 页码
         * 内容
         *
         * 多个空行。
         *
         * 按空行切会把一篇 Document 拆碎。
         */
        ParsedEvidence parsed = parseEvidence(
                normalized);

        /*
         * 如果无法识别 MCP Legal Result 的 Document 结构，
         * 才退化成安全 truncate。
         */
        if (parsed.documents()
                .isEmpty()) {

            return compactUnstructured(
                    normalized);
        }

        StringBuilder result = new StringBuilder();

        appendHeader(
                result,
                parsed.header());

        int documentLimit = Math.min(
                properties.getMaxSections(),
                parsed.documents()
                        .size());

        for (int i = 0; i < documentLimit; i++) {

            String document = parsed.documents()
                    .get(i);

            String compactedDocument = truncate(
                    document,
                    properties.getMaxSectionChars());

            appendBlock(
                    result,
                    compactedDocument);

            if (result.length() >= properties.getMaxChars()) {

                break;
            }
        }

        return truncate(
                result.toString()
                        .trim(),
                properties.getMaxChars());
    }

    private String compactUnstructured(
            String content) {

        String[] rawSections = content.split(
                "\\n\\s*\\n");

        StringBuilder result = new StringBuilder();
        int selectedSections = 0;

        for (String rawSection : rawSections) {

            if (selectedSections >= properties.getMaxSections()) {

                break;
            }

            if (rawSection == null
                    || rawSection.isBlank()) {

                continue;
            }

            String compactedSection = truncate(
                    rawSection.trim(),
                    properties.getMaxSectionChars());

            appendBlock(
                    result,
                    compactedSection);

            selectedSections++;

            if (result.length() >= properties.getMaxChars()) {

                break;
            }
        }

        if (result.isEmpty()) {

            return truncate(
                    content,
                    properties.getMaxChars());
        }

        return truncate(
                result.toString(),
                properties.getMaxChars());
    }

    private ParsedEvidence parseEvidence(
            String content) {

        Matcher matcher = DOCUMENT_PATTERN.matcher(
                content);

        List<Integer> starts = new ArrayList<>();

        while (matcher.find()) {

            starts.add(
                    matcher.start());
        }

        if (starts.isEmpty()) {

            return new ParsedEvidence(
                    "",
                    List.of());
        }

        /*
         * 第一份“参考资料”之前的内容：
         *
         * 有效检索问题
         * 检索文档数量
         *
         * 作为 header 保留。
         */
        String header = content.substring(
                0,
                starts.get(0))
                .trim();

        List<String> documents = new ArrayList<>();

        for (int i = 0; i < starts.size(); i++) {

            int start = starts.get(i);

            int end = i + 1 < starts.size()
                    ? starts.get(i + 1)
                    : content.length();

            String document = content.substring(
                    start,
                    end)
                    .trim();

            if (!document.isBlank()) {

                documents.add(
                        document);
            }
        }

        return new ParsedEvidence(
                header,
                documents);
    }

    private void appendHeader(
            StringBuilder builder,
            String header) {

        if (header == null
                || header.isBlank()) {

            return;
        }

        builder.append(
                header.trim());
    }

    private void appendBlock(
            StringBuilder builder,
            String block) {

        if (block == null
                || block.isBlank()) {

            return;
        }

        if (!builder.isEmpty()) {

            builder.append(
                    System.lineSeparator())
                    .append(
                            System.lineSeparator());
        }

        builder.append(
                block.trim());
    }

    private String normalize(
            String content) {

        return content
                .replace(
                        "\r\n",
                        "\n")
                .replace(
                        '\r',
                        '\n')
                .trim();
    }

    private String truncate(
            String value,
            int maxChars) {

        if (value == null
                || value.length() <= maxChars) {

            return value;
        }

        int targetLength = Math.max(
                0,
                maxChars
                        - TRUNCATED_SUFFIX.length());

        return value.substring(
                0,
                targetLength)
                + TRUNCATED_SUFFIX;
    }

    private record ParsedEvidence(
            String header,
            List<String> documents) {
    }
}
