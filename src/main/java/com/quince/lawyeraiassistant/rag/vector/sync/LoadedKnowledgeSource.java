package com.quince.lawyeraiassistant.rag.vector.sync;

import java.util.List;
import java.util.Objects;

import org.springframework.ai.document.Document;

public record LoadedKnowledgeSource(
        String sourceDocumentId,
        String sourceName,
        String sourceUri,
        String sourceChecksum,
        String knowledgeVersion,
        List<Document> parents,
        List<Document> chunks) {

    public LoadedKnowledgeSource {

        sourceDocumentId = requireText(
                sourceDocumentId,
                "sourceDocumentId");

        sourceName = requireText(
                sourceName,
                "sourceName");

        sourceUri = requireText(
                sourceUri,
                "sourceUri");

        sourceChecksum = requireText(
                sourceChecksum,
                "sourceChecksum");

        knowledgeVersion = requireText(
                knowledgeVersion,
                "knowledgeVersion");

        parents = parents == null
                ? List.of()
                : List.copyOf(parents);

        chunks = chunks == null
                ? List.of()
                : List.copyOf(chunks);
    }

    public List<String> parentIds() {

        return parents.stream()
                .map(Document::getId)
                .toList();
    }

    public List<String> chunkIds() {

        return chunks.stream()
                .map(Document::getId)
                .toList();
    }

    private static String requireText(
            String value,
            String fieldName) {

        Objects.requireNonNull(
                value,
                fieldName + " must not be null");

        String normalized = value.trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank");
        }

        return normalized;
    }
}