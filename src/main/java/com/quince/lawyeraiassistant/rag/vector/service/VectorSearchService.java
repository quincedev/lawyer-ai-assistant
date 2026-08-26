package com.quince.lawyeraiassistant.rag.vector.service;

import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import com.quince.lawyeraiassistant.common.exception.ErrorCode;
import com.quince.lawyeraiassistant.common.exception.KnowledgeBaseException;
import com.quince.lawyeraiassistant.rag.config.ConditionalOnRagEnabled;
import com.quince.lawyeraiassistant.retrieval.tenant.TenantKnowledgeFilterFactory;
import com.quince.lawyeraiassistant.retrieval.config.RetrievalProperties;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@ConditionalOnRagEnabled
public class VectorSearchService {

        private final VectorStore vectorStore;

        private final RetrievalProperties retrievalProperties;

        private final TenantKnowledgeFilterFactory tenantKnowledgeFilterFactory;

        public VectorSearchService(
                        VectorStore vectorStore,
                        RetrievalProperties retrievalProperties,
                        TenantKnowledgeFilterFactory tenantKnowledgeFilterFactory) {

                this.vectorStore = Objects.requireNonNull(
                                vectorStore,
                                "vectorStore must not be null");

                this.retrievalProperties = Objects.requireNonNull(
                                retrievalProperties,
                                "retrievalProperties must not be null");

                this.tenantKnowledgeFilterFactory = Objects.requireNonNull(
                                tenantKnowledgeFilterFactory,
                                "tenantKnowledgeFilterFactory must not be null");
        }

        public int addDocuments(
                        List<Document> documents) {

                if (documents == null
                                || documents.isEmpty()) {

                        return 0;
                }

                Map<String, Document> uniqueDocuments = new LinkedHashMap<>();

                for (Document document : documents) {

                        if (document == null) {
                                continue;
                        }

                        String documentId = document.getId();

                        if (documentId == null
                                        || documentId.isBlank()) {

                                throw new KnowledgeBaseException(
                                                ErrorCode.KNOWLEDGE_BASE_INITIALIZATION_ERROR,
                                                "向量文档 ID 不能为空");
                        }

                        Document existing = uniqueDocuments.putIfAbsent(
                                        documentId,
                                        document);

                        if (existing != null
                                        && !Objects.equals(
                                                        existing.getText(),
                                                        document.getText())) {

                                throw new KnowledgeBaseException(
                                                ErrorCode.KNOWLEDGE_BASE_INITIALIZATION_ERROR,
                                                "检测到冲突的向量文档 ID: "
                                                                + documentId);
                        }
                }

                List<Document> uniqueValues = List.copyOf(
                                uniqueDocuments.values());

                if (uniqueValues.isEmpty()) {
                        return 0;
                }

                vectorStore.add(
                                uniqueValues);

                return uniqueValues.size();
        }

        /**
         * Legacy/default retrieval.
         *
         * Important:
         * No tenant identity means SHARED knowledge only.
         */
        public List<Document> search(
                        String query) {

                return searchShared(
                                query);
        }

        public List<Document> searchShared(
                        String query) {

                return searchInternal(
                                query,
                                tenantKnowledgeFilterFactory
                                                .createSharedOnly());
        }

        public List<Document> searchForTenant(
                        String query,
                        String tenantId) {

                return searchInternal(
                                query,
                                tenantKnowledgeFilterFactory
                                                .createForTenant(
                                                                tenantId));
        }

        private List<Document> searchInternal(
                        String query,
                        String filterExpression) {

                if (query == null
                                || query.isBlank()) {

                        throw new IllegalArgumentException(
                                        "搜索问题不能为空");
                }

                Objects.requireNonNull(
                                filterExpression,
                                "filterExpression must not be null");

                try {

                        log.info(
                                        "Executing tenant-scoped similarity search.");

                        SearchRequest request = SearchRequest.builder()
                                        .query(
                                                        query.trim())
                                        .topK(
                                                        retrievalProperties
                                                                        .topK())
                                        .similarityThreshold(
                                                        retrievalProperties
                                                                        .similarityThreshold())
                                        .filterExpression(
                                                        filterExpression)
                                        .build();

                        List<Document> documents = vectorStore.similaritySearch(
                                        request);

                        List<Document> result = documents == null
                                        ? List.of()
                                        : List.copyOf(
                                                        documents);

                        log.info(
                                        "Similarity search completed. matchedDocuments={}",
                                        result.size());

                        return result;

                } catch (KnowledgeBaseException exception) {

                        throw exception;

                } catch (Exception exception) {

                        log.error(
                                        "Vector similarity search failed.",
                                        exception);

                        throw new KnowledgeBaseException(
                                        ErrorCode.KNOWLEDGE_BASE_VECTOR_SEARCH_ERROR,
                                        "知识库检索失败",
                                        exception);
                }
        }

        public int deleteDocuments(
                        List<String> documentIds) {

                if (documentIds == null
                                || documentIds.isEmpty()) {

                        return 0;
                }

                List<String> uniqueIds = documentIds.stream()
                                .filter(Objects::nonNull)
                                .map(String::trim)
                                .filter(id -> !id.isEmpty())
                                .distinct()
                                .toList();

                if (uniqueIds.isEmpty()) {
                        return 0;
                }

                try {
                        vectorStore.delete(
                                        uniqueIds);

                        return uniqueIds.size();
                } catch (Exception exception) {
                        throw new KnowledgeBaseException(
                                        ErrorCode.KNOWLEDGE_BASE_INITIALIZATION_ERROR,
                                        "删除过期向量文档失败",
                                        exception);
                }
        }
}
