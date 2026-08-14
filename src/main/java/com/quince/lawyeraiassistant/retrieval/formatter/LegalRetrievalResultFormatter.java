package com.quince.lawyeraiassistant.retrieval.formatter;

import com.quince.lawyeraiassistant.retrieval.model.RetrieverContext;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 法律检索结果统一格式化器。
 *
 * <p>
 * 将 Retrieval Runtime 返回的 RetrieverContext
 * 转换成适合 Agent Tool / MCP Tool 使用的文本。
 * </p>
 *
 * <p>
 * Local Agent Tool 和 MCP Tool
 * 共同复用本 Formatter，
 * 避免不同 Adapter 重复实现结果格式化逻辑。
 * </p>
 */
@Component
public class LegalRetrievalResultFormatter {

    private static final String NO_KNOWLEDGE_FOUND = "未检索到与当前法律问题相关的知识。";

    /**
     * 将 RetrieverContext 格式化为法律检索文本。
     */
    public String format(
            RetrieverContext context) {

        Objects.requireNonNull(
                context,
                "RetrieverContext must not be null");

        if (!context.hasDocuments()) {
            return NO_KNOWLEDGE_FOUND;
        }

        StringBuilder builder = new StringBuilder();

        builder.append(
                "有效检索问题：")
                .append(
                        context.effectiveQuery())
                .append(
                        System.lineSeparator());

        builder.append(
                "检索文档数量：")
                .append(
                        context.documentCount())
                .append(
                        System.lineSeparator())
                .append(
                        System.lineSeparator());

        List<Document> documents = context.getDocuments();

        for (int index = 0; index < documents.size(); index++) {

            Document document = documents.get(
                    index);

            appendDocument(
                    builder,
                    document,
                    index + 1);

            if (index < documents.size() - 1) {

                builder.append(
                        System.lineSeparator())
                        .append(
                                "---")
                        .append(
                                System.lineSeparator())
                        .append(
                                System.lineSeparator());
            }
        }

        return builder.toString()
                .trim();
    }

    /**
     * 格式化单个检索 Document。
     */
    private void appendDocument(
            StringBuilder builder,
            Document document,
            int index) {

        builder.append(
                "参考资料 ")
                .append(
                        index)
                .append(
                        System.lineSeparator());

        appendMetadata(
                builder,
                document);

        builder.append(
                "内容：")
                .append(
                        System.lineSeparator())
                .append(
                        normalizeDocumentText(
                                document))
                .append(
                        System.lineSeparator());
    }

    /**
     * 保留当前项目 RAG 数据中实际存在的 metadata。
     *
     * 当前项目使用：
     *
     * file_name
     * page_number
     * chunk_index
     */
    private void appendMetadata(
            StringBuilder builder,
            Document document) {

        Map<String, Object> metadata = document.getMetadata();

        appendMetadataIfPresent(
                builder,
                metadata,
                "file_name",
                "来源");

        appendMetadataIfPresent(
                builder,
                metadata,
                "page_number",
                "页码");

        appendMetadataIfPresent(
                builder,
                metadata,
                "chunk_index",
                "Chunk");

        if (document.getScore() != null) {

            builder.append(
                    "相关度：")
                    .append(
                            document.getScore())
                    .append(
                            System.lineSeparator());
        }
    }

    private void appendMetadataIfPresent(
            StringBuilder builder,
            Map<String, Object> metadata,
            String key,
            String label) {

        if (metadata == null
                || metadata.isEmpty()) {

            return;
        }

        Object value = metadata.get(
                key);

        if (value == null) {
            return;
        }

        String normalizedValue = value.toString()
                .trim();

        if (normalizedValue.isEmpty()) {
            return;
        }

        builder.append(
                label)
                .append(
                        "：")
                .append(
                        normalizedValue)
                .append(
                        System.lineSeparator());
    }

    private String normalizeDocumentText(
            Document document) {

        String text = document.getText();

        if (text == null
                || text.isBlank()) {

            return "[无有效文本]";
        }

        return text.trim();
    }
}