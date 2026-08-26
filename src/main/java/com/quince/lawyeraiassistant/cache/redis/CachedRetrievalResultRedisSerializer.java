package com.quince.lawyeraiassistant.cache.redis;

import com.quince.lawyeraiassistant.cache.retrieval.CachedRetrievalResult;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

public final class CachedRetrievalResultRedisSerializer
        implements RedisCacheSerializer<CachedRetrievalResult> {

    private final ObjectMapper objectMapper;
    private final RedisDocumentMapper documentMapper;

    public CachedRetrievalResultRedisSerializer(
            ObjectMapper objectMapper,
            RedisDocumentMapper documentMapper) {

        this.objectMapper = objectMapper;
        this.documentMapper = documentMapper;
    }

    @Override
    public String serialize(
            CachedRetrievalResult result) {

        if (result == null) {
            throw new IllegalArgumentException(
                    "Cached retrieval result must not be null");
        }

        try {
            RedisCachedRetrievalResultValue value = new RedisCachedRetrievalResultValue(
                    documentMapper.toRedisValues(
                            result.documents()));

            return objectMapper.writeValueAsString(
                    value);
        } catch (JacksonException exception) {
            throw new RedisCacheSerializationException(
                    "Failed to serialize retrieval cache value",
                    exception);
        }
    }

    @Override
    public CachedRetrievalResult deserialize(
            String payload) {

        if (payload == null || payload.isBlank()) {
            throw new RedisCacheSerializationException(
                    "Retrieval cache payload must not be blank");
        }

        try {
            RedisCachedRetrievalResultValue value = objectMapper.readValue(
                    payload,
                    RedisCachedRetrievalResultValue.class);

            return new CachedRetrievalResult(
                    documentMapper.toDocuments(
                            value.documents()));
        } catch (JacksonException exception) {
            throw new RedisCacheSerializationException(
                    "Failed to deserialize retrieval cache value",
                    exception);
        }
    }
}