package com.quince.lawyeraiassistant.cache.retrieval;

import java.util.Optional;

public interface RetrievalCache {

    Optional<CachedRetrievalResult> get(
            String key);

    void put(
            String key,
            CachedRetrievalResult result);

    void invalidate(
            String key);

    void clear();
}