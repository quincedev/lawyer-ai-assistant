package com.quince.lawyeraiassistant.cache.redis;

public interface RedisCacheSerializer<T> {

    String serialize(T value);

    T deserialize(String value);
}