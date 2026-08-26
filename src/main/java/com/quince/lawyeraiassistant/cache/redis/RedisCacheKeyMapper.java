package com.quince.lawyeraiassistant.cache.redis;

import com.quince.lawyeraiassistant.cache.config.AiCacheProperties;

public final class RedisCacheKeyMapper {

    private static final String TOOL_NAMESPACE = "tool";

    private static final String RETRIEVAL_NAMESPACE = "retrieval";

    private final String keyPrefix;

    public RedisCacheKeyMapper(
            AiCacheProperties properties) {

        this.keyPrefix = normalizePrefix(
                properties.getRedis().getKeyPrefix());
    }

    public String toolKey(
            String cacheKey) {

        return map(
                TOOL_NAMESPACE,
                cacheKey);
    }

    public String retrievalKey(
            String cacheKey) {

        return map(
                RETRIEVAL_NAMESPACE,
                cacheKey);
    }

    public String toolPattern() {

        return keyPrefix
                + ":"
                + TOOL_NAMESPACE
                + ":*";
    }

    public String retrievalPattern() {

        return keyPrefix
                + ":"
                + RETRIEVAL_NAMESPACE
                + ":*";
    }

    private String map(
            String namespace,
            String cacheKey) {

        if (cacheKey == null || cacheKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Cache key must not be blank");
        }

        return keyPrefix
                + ":"
                + namespace
                + ":"
                + cacheKey;
    }

    private String normalizePrefix(
            String value) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Redis cache key prefix must not be blank");
        }

        return value.replaceAll(
                "^:+|:+$",
                "");
    }
}