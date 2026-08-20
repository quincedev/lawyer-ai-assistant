package com.quince.lawyeraiassistant.cache.tool;

import java.util.Optional;

public interface ToolResultCache {

    Optional<String> get(
            String key);

    void put(
            String key,
            String result);

    void invalidate(
            String key);

    void clear();
}