package com.quince.lawyeraiassistant.cache.redis;

import java.time.Duration;
import java.util.Optional;

import com.quince.lawyeraiassistant.cache.config.AiCacheProperties;
import com.quince.lawyeraiassistant.cache.tool.ToolResultCache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;

public final class RedisToolResultCache
        implements ToolResultCache {

    private static final Logger log = LoggerFactory.getLogger(
            RedisToolResultCache.class);

    private final StringRedisTemplate redisTemplate;

    private final RedisCacheKeyMapper keyMapper;

    private final RedisCacheSerializer<String> serializer;

    private final RedisNamespaceCleaner namespaceCleaner;

    private final Duration ttl;

    private final boolean failOpen;

    public RedisToolResultCache(
            StringRedisTemplate redisTemplate,
            RedisCacheKeyMapper keyMapper,
            RedisCacheSerializer<String> serializer,
            RedisNamespaceCleaner namespaceCleaner,
            AiCacheProperties properties) {

        this.redisTemplate = redisTemplate;
        this.keyMapper = keyMapper;
        this.serializer = serializer;
        this.namespaceCleaner = namespaceCleaner;
        this.ttl = properties.getTool().getTtl();
        this.failOpen = properties.getRedis().isFailOpen();

        validateTtl(ttl);
    }

    @Override
    public Optional<String> get(
            String cacheKey) {

        String redisKey = keyMapper.toolKey(cacheKey);

        try {
            String payload = redisTemplate
                    .opsForValue()
                    .get(redisKey);

            if (payload == null) {
                return Optional.empty();
            }

            return Optional.of(
                    serializer.deserialize(payload));
        } catch (DataAccessException
                | RedisCacheSerializationException exception) {

            return handleReadFailure(
                    redisKey,
                    exception);
        }
    }

    @Override
    public void put(
            String cacheKey,
            String toolResult) {

        if (toolResult == null) {
            throw new IllegalArgumentException(
                    "Tool result must not be null");
        }

        String redisKey = keyMapper.toolKey(cacheKey);

        try {
            String payload = serializer.serialize(toolResult);

            redisTemplate
                    .opsForValue()
                    .set(
                            redisKey,
                            payload,
                            ttl);
        } catch (DataAccessException
                | RedisCacheSerializationException exception) {

            handleWriteFailure(
                    redisKey,
                    exception);
        }
    }

    @Override
    public void invalidate(
            String cacheKey) {

        /*
         * 与 CaffeineToolResultCache 保持一致。
         */
        if (cacheKey == null) {
            return;
        }

        String redisKey = keyMapper.toolKey(cacheKey);

        try {
            redisTemplate.delete(
                    redisKey);
        } catch (DataAccessException exception) {
            handleWriteFailure(
                    redisKey,
                    exception);
        }
    }

    @Override
    public void clear() {

        String pattern = keyMapper.toolPattern();

        try {
            namespaceCleaner.clear(
                    pattern);
        } catch (DataAccessException exception) {
            handleWriteFailure(
                    pattern,
                    exception);
        }
    }

    private Optional<String> handleReadFailure(
            String redisKey,
            RuntimeException exception) {

        if (!failOpen) {
            throw exception;
        }

        log.warn(
                "Redis tool cache read failed; "
                        + "treating as cache miss, key={}",
                redisKey,
                exception);

        return Optional.empty();
    }

    private void handleWriteFailure(
            String redisKey,
            RuntimeException exception) {

        if (!failOpen) {
            throw exception;
        }

        log.warn(
                "Redis tool cache operation failed; "
                        + "continuing without cache, key={}",
                redisKey,
                exception);
    }

    private void validateTtl(
            Duration value) {

        if (value == null
                || value.isZero()
                || value.isNegative()) {

            throw new IllegalArgumentException(
                    "Tool cache TTL must be positive");
        }
    }
}