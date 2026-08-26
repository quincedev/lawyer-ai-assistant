package com.quince.lawyeraiassistant.cache.redis;

public final class RedisCacheSerializationException
        extends RuntimeException {

    public RedisCacheSerializationException(
            String message) {

        super(message);
    }

    public RedisCacheSerializationException(
            String message,
            Throwable cause) {

        super(message, cause);
    }
}