package com.quince.lawyeraiassistant.cache.redis;

import java.util.List;

public record RedisCachedRetrievalResultValue(
        List<RedisDocumentValue> documents) {

    public RedisCachedRetrievalResultValue {

        documents = documents == null
                ? List.of()
                : List.copyOf(documents);
    }
}