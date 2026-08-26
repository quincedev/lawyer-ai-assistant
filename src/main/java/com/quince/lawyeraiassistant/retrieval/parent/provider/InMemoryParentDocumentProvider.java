package com.quince.lawyeraiassistant.retrieval.parent.provider;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.quince.lawyeraiassistant.rag.config.ConditionalOnRagEnabled;

/**
 * 基于内存的 Parent Document 存储与读取实现。
 *
 * <p>
 * 当前适用于单机开发和教学环境。应用重启后数据会丢失，
 * 但知识库初始化阶段会重新加载。
 * </p>
 */
@Component
@ConditionalOnRagEnabled
public class InMemoryParentDocumentProvider
        implements ParentDocumentProvider,
        ParentDocumentStore {

    private final Map<String, Document> parentDocuments = new ConcurrentHashMap<>();

    @Override
    public void save(Document document) {
        Objects.requireNonNull(
                document,
                "document must not be null");

        String documentId = validateDocumentId(
                document.getId());

        parentDocuments.put(
                documentId,
                document);
    }

    @Override
    public void saveAll(
            Collection<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }

        for (Document document : documents) {
            save(document);
        }
    }

    @Override
    public Optional<Document> findById(
            String parentDocumentId) {
        if (parentDocumentId == null
                || parentDocumentId.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                parentDocuments.get(
                        parentDocumentId.trim()));
    }

    @Override
    public Collection<Document> findAllByIds(
            Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        /*
         * LinkedHashMap 同时实现：
         * 1. 去重；
         * 2. 保留调用方 ID 的首次出现顺序。
         */
        Map<String, Document> result = new LinkedHashMap<>();

        for (String id : ids) {
            if (id == null || id.isBlank()) {
                continue;
            }

            String normalizedId = id.trim();

            Document document = parentDocuments.get(normalizedId);

            if (document != null) {
                result.putIfAbsent(
                        normalizedId,
                        document);
            }
        }

        return List.copyOf(
                result.values());
    }

    /**
     * 清空 Parent Document。
     *
     * <p>
     * 主要用于测试和手动重新加载知识库。
     * </p>
     */
    public void clear() {
        parentDocuments.clear();
    }

    public int size() {
        return parentDocuments.size();
    }

    private String validateDocumentId(
            String documentId) {
        Objects.requireNonNull(
                documentId,
                "parent document id must not be null");

        String normalizedId = documentId.trim();

        if (normalizedId.isEmpty()) {
            throw new IllegalArgumentException(
                    "parent document id must not be blank");
        }

        return normalizedId;
    }

    @Override
    public void deleteByIds(
            Collection<String> parentDocumentIds) {

        if (parentDocumentIds == null
                || parentDocumentIds.isEmpty()) {

            return;
        }

        for (String parentDocumentId : parentDocumentIds) {

            if (parentDocumentId == null
                    || parentDocumentId.isBlank()) {

                continue;
            }

            parentDocuments.remove(
                    parentDocumentId.trim());
        }
    }
}
