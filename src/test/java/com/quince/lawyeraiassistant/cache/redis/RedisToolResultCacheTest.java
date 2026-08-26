package com.quince.lawyeraiassistant.cache.redis;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import com.quince.lawyeraiassistant.cache.config.AiCacheProperties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RedisToolResultCacheTest {

    private static final String CACHE_KEY = "cache-key";
    private static final String REDIS_KEY = "test-prefix:tool:" + CACHE_KEY;
    private static final Duration TTL = Duration.ofMinutes(12);

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RedisCacheSerializer<String> serializer;
    private RedisNamespaceCleaner namespaceCleaner;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {

        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        serializer = mock(RedisCacheSerializer.class);
        namespaceCleaner = mock(RedisNamespaceCleaner.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void shouldReturnOriginalToolResultOnRedisHit() {

        RedisToolResultCache cache = cache(true, TTL);
        when(valueOperations.get(REDIS_KEY)).thenReturn("serialized-result");
        when(serializer.deserialize("serialized-result")).thenReturn("original-result");

        assertEquals("original-result", cache.get(CACHE_KEY).orElseThrow());
    }

    @Test
    void shouldReturnMissWhenRedisReturnsNull() {

        RedisToolResultCache cache = cache(true, TTL);
        when(valueOperations.get(REDIS_KEY)).thenReturn(null);

        assertTrue(cache.get(CACHE_KEY).isEmpty());
    }

    @Test
    void shouldTreatEmptyStringAsHit() {

        RedisToolResultCache cache = cache(true, TTL);
        when(valueOperations.get(REDIS_KEY)).thenReturn("");
        when(serializer.deserialize("")).thenReturn("");

        assertEquals("", cache.get(CACHE_KEY).orElseThrow());
    }

    @Test
    void shouldPutUsingCorrectRedisKey() {

        RedisToolResultCache cache = cache(true, TTL);
        when(serializer.serialize("tool-result")).thenReturn("payload");

        cache.put(CACHE_KEY, "tool-result");

        verify(valueOperations).set(REDIS_KEY, "payload", TTL);
    }

    @Test
    void shouldPutUsingConfiguredTtl() {

        Duration configuredTtl = Duration.ofSeconds(37);
        RedisToolResultCache cache = cache(true, configuredTtl);
        when(serializer.serialize("tool-result")).thenReturn("payload");

        cache.put(CACHE_KEY, "tool-result");

        verify(valueOperations).set(REDIS_KEY, "payload", configuredTtl);
    }

    @Test
    void shouldInvalidateCorrectRedisKey() {

        RedisToolResultCache cache = cache(true, TTL);

        cache.invalidate(CACHE_KEY);

        verify(redisTemplate).delete(REDIS_KEY);
    }

    @Test
    void shouldClearOnlyToolNamespace() {

        RedisToolResultCache cache = cache(true, TTL);

        cache.clear();

        verify(namespaceCleaner).clear("test-prefix:tool:*");
    }

    @Test
    void shouldReturnMissWhenFailOpenReadFails() {

        RedisToolResultCache cache = cache(true, TTL);
        when(valueOperations.get(REDIS_KEY)).thenThrow(redisFailure());

        assertTrue(cache.get(CACHE_KEY).isEmpty());
    }

    @Test
    void shouldNotPropagateFailOpenWriteFailure() {

        RedisToolResultCache cache = cache(true, TTL);
        when(serializer.serialize("tool-result")).thenReturn("payload");
        doThrow(redisFailure()).when(valueOperations).set(REDIS_KEY, "payload", TTL);

        assertDoesNotThrow(() -> cache.put(CACHE_KEY, "tool-result"));
    }

    @Test
    void shouldNotPropagateFailOpenDeleteFailure() {

        RedisToolResultCache cache = cache(true, TTL);
        when(redisTemplate.delete(REDIS_KEY)).thenThrow(redisFailure());

        assertDoesNotThrow(() -> cache.invalidate(CACHE_KEY));
    }

    @Test
    void shouldNotPropagateFailOpenClearFailure() {

        RedisToolResultCache cache = cache(true, TTL);
        doThrow(redisFailure())
                .when(namespaceCleaner)
                .clear("test-prefix:tool:*");

        assertDoesNotThrow(cache::clear);
    }

    @Test
    void shouldPropagateFailFastReadFailure() {

        RedisToolResultCache cache = cache(false, TTL);
        DataAccessException failure = redisFailure();
        when(valueOperations.get(REDIS_KEY)).thenThrow(failure);

        assertSame(failure, assertThrows(
                DataAccessException.class,
                () -> cache.get(CACHE_KEY)));
    }

    @Test
    void shouldPropagateFailFastWriteFailure() {

        RedisToolResultCache cache = cache(false, TTL);
        DataAccessException failure = redisFailure();
        when(serializer.serialize("tool-result")).thenReturn("payload");
        doThrow(failure).when(valueOperations).set(REDIS_KEY, "payload", TTL);

        assertSame(failure, assertThrows(
                DataAccessException.class,
                () -> cache.put(CACHE_KEY, "tool-result")));
    }

    @Test
    void shouldPropagateFailFastClearFailure() {

        RedisToolResultCache cache = cache(false, TTL);
        DataAccessException failure = redisFailure();
        doThrow(failure)
                .when(namespaceCleaner)
                .clear("test-prefix:tool:*");

        assertSame(failure, assertThrows(
                DataAccessException.class,
                cache::clear));
    }

    @Test
    void shouldRejectInvalidTtlDuringConstruction() {

        assertThrows(IllegalArgumentException.class, () -> cache(true, null));
        assertThrows(IllegalArgumentException.class, () -> cache(true, Duration.ZERO));
        assertThrows(
                IllegalArgumentException.class,
                () -> cache(true, Duration.ofSeconds(-1)));
    }

    private RedisToolResultCache cache(
            boolean failOpen,
            Duration ttl) {

        AiCacheProperties properties = new AiCacheProperties();
        properties.getRedis().setKeyPrefix("test-prefix");
        properties.getRedis().setFailOpen(failOpen);
        properties.getTool().setTtl(ttl);

        return new RedisToolResultCache(
                redisTemplate,
                new RedisCacheKeyMapper(properties),
                serializer,
                namespaceCleaner,
                properties);
    }

    private DataAccessException redisFailure() {

        return mock(DataAccessException.class);
    }
}
