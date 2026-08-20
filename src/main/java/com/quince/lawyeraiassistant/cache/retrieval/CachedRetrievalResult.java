package com.quince.lawyeraiassistant.cache.retrieval;

import java.util.List;

import org.springframework.ai.document.Document;

public record CachedRetrievalResult(
        List<Document> documents) {

    public CachedRetrievalResult {

        documents = documents == null
                ? List.of()
                : List.copyOf(
                        documents);
    }

    public boolean hasDocuments() {

        return !documents.isEmpty();
    }

    public int documentCount() {

        return documents.size();
    }
}