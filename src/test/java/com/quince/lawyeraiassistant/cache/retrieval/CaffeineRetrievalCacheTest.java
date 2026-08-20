package com.quince.lawyeraiassistant.cache.retrieval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

class CaffeineRetrievalCacheTest {

    @Test
    void shouldPutAndGetCachedRetrievalResult() {

        RetrievalCache cache = new CaffeineRetrievalCache(
                100,
                Duration.ofMinutes(
                        10));

        CachedRetrievalResult result = new CachedRetrievalResult(
                List.of(
                        Document.builder()
                                .text(
                                        "劳动合同法")
                                .build()));

        cache.put(
                "key-1",
                result);

        CachedRetrievalResult cached = cache.get(
                "key-1")
                .orElseThrow();

        assertEquals(
                1,
                cached.documentCount());
    }

    @Test
    void shouldReturnEmptyForMissingKey() {

        RetrievalCache cache = new CaffeineRetrievalCache(
                100,
                Duration.ofMinutes(
                        10));

        assertTrue(
                cache.get(
                        "missing")
                        .isEmpty());
    }

    @Test
    void shouldInvalidateOneEntryAndClearAllEntries() {

        RetrievalCache cache = cache();
        CachedRetrievalResult result = new CachedRetrievalResult(List.of());

        cache.put("one", result);
        cache.put("two", result);
        cache.invalidate("one");

        assertTrue(cache.get("one").isEmpty());
        assertTrue(cache.get("two").isPresent());

        cache.clear();

        assertTrue(cache.get("two").isEmpty());
        assertDoesNotThrow(() -> cache.invalidate(null));
    }

    @Test
    void shouldRejectInvalidConstructorAndOperationArguments() {

        assertThrows(IllegalArgumentException.class,
                () -> new CaffeineRetrievalCache(0, Duration.ofMinutes(1)));
        assertThrows(NullPointerException.class,
                () -> new CaffeineRetrievalCache(1, null));
        assertThrows(IllegalArgumentException.class,
                () -> new CaffeineRetrievalCache(1, Duration.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> new CaffeineRetrievalCache(1, Duration.ofSeconds(-1)));

        RetrievalCache cache = cache();
        assertThrows(NullPointerException.class, () -> cache.get(null));
        assertThrows(NullPointerException.class,
                () -> cache.put(null, new CachedRetrievalResult(List.of())));
        assertThrows(NullPointerException.class, () -> cache.put("key", null));
    }

    @Test
    void shouldExpireResultAfterTtl() {

        AtomicLong time = new AtomicLong();
        RetrievalCache cache = new CaffeineRetrievalCache(
                100,
                Duration.ofSeconds(10),
                time::get);

        cache.put("key", new CachedRetrievalResult(List.of()));
        time.addAndGet(TimeUnit.SECONDS.toNanos(9));
        assertTrue(cache.get("key").isPresent());

        time.addAndGet(TimeUnit.SECONDS.toNanos(1));
        assertTrue(cache.get("key").isEmpty());
    }

    private RetrievalCache cache() {

        return new CaffeineRetrievalCache(100, Duration.ofMinutes(10));
    }
}
