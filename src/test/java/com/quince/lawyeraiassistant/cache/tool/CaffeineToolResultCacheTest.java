package com.quince.lawyeraiassistant.cache.tool;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

class CaffeineToolResultCacheTest {

    @Test
    void shouldPutGetInvalidateAndClearResults() {

        ToolResultCache cache = cache();

        cache.put("one", "result-1");
        cache.put("two", "result-2");

        assertEquals("result-1", cache.get("one").orElseThrow());
        assertTrue(cache.get("missing").isEmpty());

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
                () -> new CaffeineToolResultCache(0, Duration.ofMinutes(1)));
        assertThrows(NullPointerException.class,
                () -> new CaffeineToolResultCache(1, null));
        assertThrows(IllegalArgumentException.class,
                () -> new CaffeineToolResultCache(1, Duration.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> new CaffeineToolResultCache(1, Duration.ofSeconds(-1)));

        ToolResultCache cache = cache();
        assertThrows(NullPointerException.class, () -> cache.get(null));
        assertThrows(NullPointerException.class, () -> cache.put(null, "result"));
        assertThrows(NullPointerException.class, () -> cache.put("key", null));
    }

    @Test
    void shouldPreserveEmptyResultAccordingToCurrentCacheContract() {

        ToolResultCache cache = cache();
        cache.put("key", "");

        assertEquals("", cache.get("key").orElseThrow());
    }

    @Test
    void shouldExpireResultAfterTtl() {

        AtomicLong time = new AtomicLong();
        ToolResultCache cache = new CaffeineToolResultCache(
                100,
                Duration.ofSeconds(10),
                time::get);

        cache.put("key", "result");
        time.addAndGet(TimeUnit.SECONDS.toNanos(9));
        assertTrue(cache.get("key").isPresent());

        time.addAndGet(TimeUnit.SECONDS.toNanos(1));
        assertTrue(cache.get("key").isEmpty());
    }

    private ToolResultCache cache() {

        return new CaffeineToolResultCache(100, Duration.ofMinutes(10));
    }
}
