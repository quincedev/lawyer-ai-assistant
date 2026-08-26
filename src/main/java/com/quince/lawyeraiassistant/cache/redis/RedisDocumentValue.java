package com.quince.lawyeraiassistant.cache.redis;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record RedisDocumentValue(
        String id,
        String text,
        Map<String, Object> metadata,
        Double score) {

    public RedisDocumentValue {

        metadata = metadata == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(
                        new LinkedHashMap<>(metadata));
    }
}