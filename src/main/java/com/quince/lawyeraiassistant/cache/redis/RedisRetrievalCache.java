package com.quince.lawyeraiassistant.cache.redis;

import java.time.Duration;
import java.util.Optional;

import com.quince.lawyeraiassistant.cache.config.AiCacheProperties;
import com.quince.lawyeraiassistant.cache.retrieval.CachedRetrievalResult;
import com.quince.lawyeraiassistant.cache.retrieval.RetrievalCache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;

public final class RedisRetrievalCache
        implements RetrievalCache {

    private static final Logger log = LoggerFactory.getLogger(
            RedisRetrievalCache.class);

    private final StringRedisTemplate redisTemplate;

    private final RedisCacheKeyMapper keyMapper;

    private final RedisCacheSerializer<CachedRetrievalResult> serializer;

    private final RedisNamespaceCleaner namespaceCleaner;

    private final Duration ttl;

    private final boolean failOpen;

    public RedisRetrievalCache(
            StringRedisTemplate redisTemplate,
            RedisCacheKeyMapper keyMapper,
            RedisCacheSerializer<CachedRetrievalResult> serializer,
            RedisNamespaceCleaner namespaceCleaner,
            AiCacheProperties properties) {

        this.redisTemplate = redisTemplate;
        this.keyMapper = keyMapper;
        this.serializer = serializer;
        this.namespaceCleaner = namespaceCleaner;
        this.ttl = properties.getRetrieval().getTtl();
        this.failOpen = properties.getRedis().isFailOpen();

        validateTtl(ttl);
    }

    @Override
    public Optional<CachedRetrievalResult> get(
            String cacheKey) {

        String redisKey = keyMapper.retrievalKey(cacheKey);

        final String payload;

        try {
            payload = redisTemplate
                    .opsForValue()
                    .get(redisKey);
        } catch (DataAccessException exception) {
            return handleRedisReadFailure(
                    redisKey,
                    exception);
        }

        if (payload == null) {
            return Optional.empty();
        }

        try {
            CachedRetrievalResult result = serializer.deserialize(payload);

            return Optional.of(result);
        } catch (RedisCacheSerializationException exception) {
            return handleCorruptedPayload(
                    redisKey,
                    exception);
        }
    }

    @Override
    public void put(
            String cacheKey,
            CachedRetrievalResult result) {

        if (result == null) {
            throw new IllegalArgumentException(
                    "Cached retrieval result must not be null");
        }

        String redisKey = keyMapper.retrievalKey(cacheKey);

        try {
            String payload = serializer.serialize(result);

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
         * 与 CaffeineRetrievalCache 保持一致：
         * invalidate(null) 直接忽略。
         */
        if (cacheKey == null) {
            return;
        }

        String redisKey = keyMapper.retrievalKey(cacheKey);

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

        String pattern = keyMapper.retrievalPattern();

        try {
            namespaceCleaner.clear(
                    pattern);
        } catch (DataAccessException exception) {
            handleWriteFailure(
                    pattern,
                    exception);
        }
    }

    private Optional<CachedRetrievalResult> handleRedisReadFailure(
            String redisKey,
            DataAccessException exception) {

        if (!failOpen) {
            throw exception;
        }

        log.warn(
                "Redis retrieval cache read failed; "
                        + "treating as cache miss, key={}",
                redisKey,
                exception);

        return Optional.empty();
    }

    private Optional<CachedRetrievalResult> handleCorruptedPayload(
            String redisKey,
            RedisCacheSerializationException exception) {

        if (!failOpen) {
            throw exception;
        }

        log.warn(
                "Corrupted Redis retrieval cache payload; "
                        + "deleting key and treating as cache miss, key={}",
                redisKey,
                exception);

        deleteCorruptedKey(
                redisKey);

        return Optional.empty();
    }

    private void deleteCorruptedKey(
            String redisKey) {

        try {
            redisTemplate.delete(
                    redisKey);
        } catch (DataAccessException deleteException) {
            /*
             * 删除失败不能覆盖原始反序列化故障，
             * fail-open 模式继续按缓存 MISS 处理。
             */
            log.warn(
                    "Failed to delete corrupted Redis "
                            + "retrieval cache key, key={}",
                    redisKey,
                    deleteException);
        }
    }

    private void handleWriteFailure(
            String redisKey,
            RuntimeException exception) {

        if (!failOpen) {
            throw exception;
        }

        log.warn(
                "Redis retrieval cache operation failed; "
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
                    "Retrieval cache TTL must be positive");
        }
    }
}