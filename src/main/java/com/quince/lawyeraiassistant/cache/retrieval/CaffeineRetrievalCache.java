package com.quince.lawyeraiassistant.cache.retrieval;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;

public class CaffeineRetrievalCache
        implements RetrievalCache {

    private final Cache<String, CachedRetrievalResult> cache;

    public CaffeineRetrievalCache(
            long maximumSize,
            Duration ttl) {

        this(maximumSize, ttl, Ticker.systemTicker());
    }

    CaffeineRetrievalCache(
            long maximumSize,
            Duration ttl,
            Ticker ticker) {

        if (maximumSize <= 0) {

            throw new IllegalArgumentException(
                    "maximumSize must be positive");
        }

        Objects.requireNonNull(
                ttl,
                "ttl must not be null");

        Objects.requireNonNull(
                ticker,
                "ticker must not be null");

        if (ttl.isZero()
                || ttl.isNegative()) {

            throw new IllegalArgumentException(
                    "ttl must be positive");
        }

        this.cache = Caffeine.newBuilder()
                .maximumSize(
                        maximumSize)
                .expireAfterWrite(
                        ttl)
                .ticker(
                        ticker)
                .build();
    }

    @Override
    public Optional<CachedRetrievalResult> get(
            String key) {

        Objects.requireNonNull(
                key,
                "key must not be null");

        return Optional.ofNullable(
                cache.getIfPresent(
                        key));
    }

    @Override
    public void put(
            String key,
            CachedRetrievalResult result) {

        Objects.requireNonNull(
                key,
                "key must not be null");

        Objects.requireNonNull(
                result,
                "result must not be null");

        cache.put(
                key,
                result);
    }

    @Override
    public void invalidate(
            String key) {

        if (key == null) {

            return;
        }

        cache.invalidate(
                key);
    }

    @Override
    public void clear() {

        cache.invalidateAll();
    }
}
