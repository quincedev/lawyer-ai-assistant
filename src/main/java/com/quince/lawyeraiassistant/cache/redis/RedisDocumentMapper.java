package com.quince.lawyeraiassistant.cache.redis;

import java.util.List;

import org.springframework.ai.document.Document;

public final class RedisDocumentMapper {

    private final RedisMetadataValidator metadataValidator;

    public RedisDocumentMapper(
            RedisMetadataValidator metadataValidator) {

        this.metadataValidator = metadataValidator;
    }

    public RedisDocumentValue toRedisValue(
            Document document) {

        if (document == null) {
            throw new IllegalArgumentException(
                    "Document must not be null");
        }

        metadataValidator.validate(document.getMetadata());

        return new RedisDocumentValue(
                document.getId(),
                document.getText(),
                document.getMetadata(),
                document.getScore());
    }

    public Document toDocument(
            RedisDocumentValue value) {

        if (value == null) {
            throw new IllegalArgumentException(
                    "Redis document value must not be null");
        }

        return Document.builder()
                .id(value.id())
                .text(value.text())
                .metadata(value.metadata())
                .score(value.score())
                .build();
    }

    public List<RedisDocumentValue> toRedisValues(
            List<Document> documents) {

        if (documents == null) {
            throw new IllegalArgumentException(
                    "Documents must not be null");
        }

        return documents.stream()
                .map(this::toRedisValue)
                .toList();
    }

    public List<Document> toDocuments(
            List<RedisDocumentValue> values) {

        if (values == null) {
            throw new IllegalArgumentException(
                    "Redis document values must not be null");
        }

        return values.stream()
                .map(this::toDocument)
                .toList();
    }
}