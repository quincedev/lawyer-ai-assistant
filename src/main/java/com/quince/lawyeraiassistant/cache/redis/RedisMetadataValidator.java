package com.quince.lawyeraiassistant.cache.redis;

import java.util.List;
import java.util.Map;

public final class RedisMetadataValidator {

    public void validate(
            Map<String, Object> metadata) {

        if (metadata == null) {
            return;
        }

        metadata.forEach((key, value) -> {
            if (key == null || key.isBlank()) {
                throw new RedisCacheSerializationException(
                        "Document metadata key must not be blank");
            }

            validateValue(key, value);
        });
    }

    private void validateValue(
            String key,
            Object value) {

        if (value == null
                || value instanceof String
                || value instanceof Number
                || value instanceof Boolean) {
            return;
        }

        if (value instanceof List<?> values) {
            values.forEach(item -> validateValue(key, item));
            return;
        }

        if (value instanceof Map<?, ?> values) {
            values.forEach((nestedKey, nestedValue) -> {
                if (!(nestedKey instanceof String)) {
                    throw unsupported(key, value);
                }

                validateValue(key, nestedValue);
            });
            return;
        }

        throw unsupported(key, value);
    }

    private RedisCacheSerializationException unsupported(
            String key,
            Object value) {

        return new RedisCacheSerializationException(
                "Unsupported metadata type for key '%s': %s"
                        .formatted(
                                key,
                                value.getClass().getName()));
    }
}