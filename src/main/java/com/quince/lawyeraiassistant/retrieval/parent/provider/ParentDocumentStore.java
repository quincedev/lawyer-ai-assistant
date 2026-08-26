package com.quince.lawyeraiassistant.retrieval.parent.provider;

import java.util.Collection;

import org.springframework.ai.document.Document;

public interface ParentDocumentStore {

    void save(
            Document document);

    void saveAll(
            Collection<Document> documents);

    void deleteByIds(
            Collection<String> parentDocumentIds);
}