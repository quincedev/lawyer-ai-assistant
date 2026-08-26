package com.quince.lawyeraiassistant.cache.redis;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

public final class RedisNamespaceCleaner {

    private static final long SCAN_COUNT = 500;

    private final StringRedisTemplate redisTemplate;

    public RedisNamespaceCleaner(
            StringRedisTemplate redisTemplate) {

        this.redisTemplate = redisTemplate;
    }

    public void clear(
            String pattern) {

        if (pattern == null || pattern.isBlank()) {
            throw new IllegalArgumentException(
                    "Redis scan pattern must not be blank");
        }

        redisTemplate.execute(
                (RedisConnection connection) -> {

                    ScanOptions options = ScanOptions.scanOptions()
                            .match(pattern)
                            .count(SCAN_COUNT)
                            .build();

                    try (Cursor<byte[]> cursor = connection.keyCommands()
                            .scan(options)) {

                        List<byte[]> keys = new ArrayList<>();

                        while (cursor.hasNext()) {
                            keys.add(cursor.next());

                            if (keys.size() >= SCAN_COUNT) {
                                deleteBatch(
                                        connection,
                                        keys);
                            }
                        }

                        deleteBatch(
                                connection,
                                keys);
                    }

                    return null;
                });
    }

    private void deleteBatch(
            RedisConnection connection,
            List<byte[]> keys) {

        if (keys.isEmpty()) {
            return;
        }

        connection.keyCommands()
                .del(keys.toArray(byte[][]::new));

        keys.clear();
    }
}