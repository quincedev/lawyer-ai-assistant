package com.quince.lawyeraiassistant.rag.vector.service;

import java.util.List;
import java.util.Objects;

import org.springframework.ai.document.Document;

record LoadedKnowledgeResource(
        String sourceName,
        String sourceUri,
        String sourceChecksum,
        List<Document> parents) {

    LoadedKnowledgeResource {

        sourceName = requireText(
                sourceName,
                "sourceName");

        sourceUri = requireText(
                sourceUri,
                "sourceUri");

        sourceChecksum = requireText(
                sourceChecksum,
                "sourceChecksum");

        parents = parents == null
                ? List.of()
                : List.copyOf(parents);
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