package com.quince.lawyeraiassistant.cache.redis;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;

import com.quince.lawyeraiassistant.cache.config.AiCacheProperties;
import com.quince.lawyeraiassistant.cache.retrieval.CachedRetrievalResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RedisRetrievalCacheTest {

    private static final String CACHE_KEY = "cache-key";

    private static final String REDIS_KEY = "test-prefix:retrieval:" + CACHE_KEY;

    private static final String RETRIEVAL_PATTERN = "test-prefix:retrieval:*";

    private static final Duration TTL = Duration.ofMinutes(12);

    private StringRedisTemplate redisTemplate;

    private ValueOperations<String, String> valueOperations;

    private RedisCacheSerializer<CachedRetrievalResult> serializer;

    private RedisNamespaceCleaner namespaceCleaner;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {

        redisTemplate = mock(StringRedisTemplate.class);

        valueOperations = mock(ValueOperations.class);

        serializer = mock(RedisCacheSerializer.class);

        namespaceCleaner = mock(RedisNamespaceCleaner.class);

        when(redisTemplate.opsForValue())
                .thenReturn(valueOperations);
    }

    @Test
    void shouldReturnCachedResultOnRedisHit() {

        RedisRetrievalCache cache = cache(
                true,
                TTL);

        CachedRetrievalResult expected = new CachedRetrievalResult(
                List.of());

        when(valueOperations.get(REDIS_KEY))
                .thenReturn(
                        "{\"documents\":[]}");

        when(serializer.deserialize(
                "{\"documents\":[]}"))
                .thenReturn(expected);

        CachedRetrievalResult actual = cache.get(CACHE_KEY)
                .orElseThrow();

        assertSame(
                expected,
                actual);

        assertFalse(
                actual.hasDocuments());
    }

    @Test
    void shouldReturnMissWhenRedisReturnsNull() {

        RedisRetrievalCache cache = cache(
                true,
                TTL);

        when(valueOperations.get(REDIS_KEY))
                .thenReturn(null);

        assertTrue(
                cache.get(CACHE_KEY)
                        .isEmpty());
    }

    @Test
    void shouldTreatEmptyResultAsCacheHit() {

        RedisRetrievalCache cache = cache(
                true,
                TTL);

        CachedRetrievalResult emptyResult = new CachedRetrievalResult(
                List.of());

        when(valueOperations.get(REDIS_KEY))
                .thenReturn(
                        "{\"documents\":[]}");

        when(serializer.deserialize(
                "{\"documents\":[]}"))
                .thenReturn(emptyResult);

        assertTrue(
                cache.get(CACHE_KEY)
                        .isPresent());

        assertFalse(
                cache.get(CACHE_KEY)
                        .orElseThrow()
                        .hasDocuments());
    }

    @Test
    void shouldPutResultUsingConfiguredKeyAndTtl() {

        RedisRetrievalCache cache = cache(
                true,
                TTL);

        CachedRetrievalResult result = new CachedRetrievalResult(
                List.of());

        when(serializer.serialize(result))
                .thenReturn(
                        "{\"documents\":[]}");

        cache.put(
                CACHE_KEY,
                result);

        verify(valueOperations)
                .set(
                        REDIS_KEY,
                        "{\"documents\":[]}",
                        TTL);
    }

    @Test
    void shouldInvalidateCorrectRedisKey() {

        RedisRetrievalCache cache = cache(
                true,
                TTL);

        cache.invalidate(
                CACHE_KEY);

        verify(redisTemplate)
                .delete(
                        REDIS_KEY);
    }

    @Test
    void shouldIgnoreNullKeyWhenInvalidating() {

        RedisRetrievalCache cache = cache(
                true,
                TTL);

        assertDoesNotThrow(
                () -> cache.invalidate(null));
    }

    @Test
    void shouldClearOnlyRetrievalNamespace() {

        RedisRetrievalCache cache = cache(
                true,
                TTL);

        cache.clear();

        verify(namespaceCleaner)
                .clear(
                        RETRIEVAL_PATTERN);
    }

    @Test
    void shouldReturnMissWhenFailOpenReadFails() {

        RedisRetrievalCache cache = cache(
                true,
                TTL);

        when(valueOperations.get(REDIS_KEY))
                .thenThrow(
                        redisFailure());

        assertTrue(
                cache.get(CACHE_KEY)
                        .isEmpty());
    }

    @Test
    void shouldDeleteCorruptedPayloadAndReturnMiss() {

        RedisRetrievalCache cache = cache(
                true,
                TTL);

        when(valueOperations.get(REDIS_KEY))
                .thenReturn(
                        "broken-json");

        when(serializer.deserialize(
                "broken-json"))
                .thenThrow(
                        new RedisCacheSerializationException(
                                "invalid"));

        assertTrue(
                cache.get(CACHE_KEY)
                        .isEmpty());

        verify(redisTemplate)
                .delete(
                        REDIS_KEY);
    }

    @Test
    void shouldReturnMissWhenCorruptedKeyCannotBeDeleted() {

        RedisRetrievalCache cache = cache(
                true,
                TTL);

        when(valueOperations.get(REDIS_KEY))
                .thenReturn(
                        "broken-json");

        when(serializer.deserialize(
                "broken-json"))
                .thenThrow(
                        new RedisCacheSerializationException(
                                "invalid"));

        when(redisTemplate.delete(REDIS_KEY))
                .thenThrow(
                        redisFailure());

        assertTrue(
                cache.get(CACHE_KEY)
                        .isEmpty());
    }

    @Test
    void shouldNotPropagateFailOpenWriteFailure() {

        RedisRetrievalCache cache = cache(
                true,
                TTL);

        CachedRetrievalResult result = new CachedRetrievalResult(
                List.of());

        when(serializer.serialize(result))
                .thenReturn(
                        "{\"documents\":[]}");

        doThrow(redisFailure())
                .when(valueOperations)
                .set(
                        REDIS_KEY,
                        "{\"documents\":[]}",
                        TTL);

        assertDoesNotThrow(
                () -> cache.put(
                        CACHE_KEY,
                        result));
    }

    @Test
    void shouldNotPropagateFailOpenClearFailure() {

        RedisRetrievalCache cache = cache(
                true,
                TTL);

        doThrow(redisFailure())
                .when(namespaceCleaner)
                .clear(
                        RETRIEVAL_PATTERN);

        assertDoesNotThrow(
                cache::clear);
    }

    @Test
    void shouldPropagateReadFailureWhenFailOpenIsDisabled() {

        RedisRetrievalCache cache = cache(
                false,
                TTL);

        DataAccessException failure = redisFailure();

        when(valueOperations.get(REDIS_KEY))
                .thenThrow(failure);

        assertSame(
                failure,
                assertThrows(
                        DataAccessException.class,
                        () -> cache.get(CACHE_KEY)));
    }

    @Test
    void shouldPropagateCorruptedPayloadWhenFailOpenIsDisabled() {

        RedisRetrievalCache cache = cache(
                false,
                TTL);

        RedisCacheSerializationException failure = new RedisCacheSerializationException(
                "invalid");

        when(valueOperations.get(REDIS_KEY))
                .thenReturn(
                        "broken-json");

        when(serializer.deserialize(
                "broken-json"))
                .thenThrow(failure);

        assertSame(
                failure,
                assertThrows(
                        RedisCacheSerializationException.class,
                        () -> cache.get(CACHE_KEY)));
    }

    @Test
    void shouldRejectNullResult() {

        RedisRetrievalCache cache = cache(
                true,
                TTL);

        assertThrows(
                IllegalArgumentException.class,
                () -> cache.put(
                        CACHE_KEY,
                        null));
    }

    @Test
    void shouldRejectInvalidTtl() {

        assertThrows(
                IllegalArgumentException.class,
                () -> cache(
                        true,
                        null));

        assertThrows(
                IllegalArgumentException.class,
                () -> cache(
                        true,
                        Duration.ZERO));

        assertThrows(
                IllegalArgumentException.class,
                () -> cache(
                        true,
                        Duration.ofSeconds(-1)));
    }

    private RedisRetrievalCache cache(
            boolean failOpen,
            Duration ttl) {

        AiCacheProperties properties = new AiCacheProperties();

        properties.getRedis()
                .setKeyPrefix(
                        "test-prefix");

        properties.getRedis()
                .setFailOpen(
                        failOpen);

        properties.getRetrieval()
                .setTtl(
                        ttl);

        return new RedisRetrievalCache(
                redisTemplate,
                new RedisCacheKeyMapper(
                        properties),
                serializer,
                namespaceCleaner,
                properties);
    }

    private DataAccessException redisFailure() {

        return mock(
                DataAccessException.class);
    }
}