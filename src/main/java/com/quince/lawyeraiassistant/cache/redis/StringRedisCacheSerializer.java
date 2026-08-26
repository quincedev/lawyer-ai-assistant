package com.quince.lawyeraiassistant.cache.redis;

public final class StringRedisCacheSerializer
        implements RedisCacheSerializer<String> {

    @Override
    public String serialize(
            String value) {

        if (value == null) {
            throw new IllegalArgumentException(
                    "Redis cache value must not be null");
        }

        return value;
    }

    @Override
    public String deserialize(
            String value) {

        if (value == null) {
            throw new IllegalArgumentException(
                    "Redis cache payload must not be null");
        }

        return value;
    }
}