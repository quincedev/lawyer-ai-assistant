package com.quince.lawyeraiassistant.rag.vector.service;

import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import com.quince.lawyeraiassistant.common.exception.ErrorCode;
import com.quince.lawyeraiassistant.common.exception.KnowledgeBaseException;
import com.quince.lawyeraiassistant.rag.config.RetrievalProperties;
import com.quince.lawyeraiassistant.rag.vector.tenant.TenantKnowledgeFilterFactory;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
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

        try {

            vectorStore.add(
                    documents);

            return documents.size();

        } catch (Exception exception) {

            log.error(
                    "Failed to write documents to vector store. documentCount={}",
                    documents.size(),
                    exception);

            throw new KnowledgeBaseException(
                    ErrorCode.KNOWLEDGE_BASE_VECTOR_WRITE_ERROR,
                    "知识库向量写入失败",
                    exception);
        }
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
}