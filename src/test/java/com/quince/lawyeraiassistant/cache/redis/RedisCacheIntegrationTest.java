package com.quince.lawyeraiassistant.cache.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.quince.lawyeraiassistant.cache.config.AiCacheProperties;
import com.quince.lawyeraiassistant.cache.retrieval.CachedRetrievalResult;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.ai.document.Document;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import tools.jackson.databind.ObjectMapper;

@Testcontainers
class RedisCacheIntegrationTest {

    private static final String KEY_PREFIX = "lawyer-ai-test";

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse(
                    "redis:7-alpine"))
            .withExposedPorts(6379);

    private static LettuceConnectionFactory connectionFactory;

    private static StringRedisTemplate redisTemplate;

    private RedisCacheKeyMapper keyMapper;

    private RedisNamespaceCleaner namespaceCleaner;

    private RedisToolResultCache toolCache;

    private RedisRetrievalCache retrievalCache;

    @BeforeAll
    static void createRedisClient() {

        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(
                REDIS.getHost(),
                REDIS.getMappedPort(6379));

        connectionFactory = new LettuceConnectionFactory(
                configuration);

        connectionFactory.afterPropertiesSet();
        connectionFactory.start();

        redisTemplate = new StringRedisTemplate(
                connectionFactory);

        redisTemplate.afterPropertiesSet();
    }

    @AfterAll
    static void closeRedisClient() {

        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @BeforeEach
    void setUp() throws Exception {

        /*
         * 每个测试使用干净的 Redis Database。
         */
        REDIS.execInContainer(
                "redis-cli",
                "FLUSHDB");

        AiCacheProperties properties = properties(
                Duration.ofMinutes(5),
                Duration.ofMinutes(5),
                true);

        keyMapper = new RedisCacheKeyMapper(
                properties);

        namespaceCleaner = new RedisNamespaceCleaner(
                redisTemplate);

        toolCache = new RedisToolResultCache(
                redisTemplate,
                keyMapper,
                new StringRedisCacheSerializer(),
                namespaceCleaner,
                properties);

        RedisMetadataValidator metadataValidator = new RedisMetadataValidator();

        RedisDocumentMapper documentMapper = new RedisDocumentMapper(
                metadataValidator);

        CachedRetrievalResultRedisSerializer serializer = new CachedRetrievalResultRedisSerializer(
                new ObjectMapper(),
                documentMapper);

        retrievalCache = new RedisRetrievalCache(
                redisTemplate,
                keyMapper,
                serializer,
                namespaceCleaner,
                properties);
    }

    @Test
    void shouldRoundTripToolResultThroughRealRedis() {

        toolCache.put(
                "tool-key",
                "raw-mcp-result");

        Optional<String> result = toolCache.get(
                "tool-key");

        assertTrue(
                result.isPresent());

        assertEquals(
                "raw-mcp-result",
                result.orElseThrow());
    }

    @Test
    void shouldStoreToolResultWithTtl() {

        toolCache.put(
                "tool-key",
                "raw-mcp-result");

        String redisKey = keyMapper.toolKey(
                "tool-key");

        Long ttl = redisTemplate.getExpire(
                redisKey,
                TimeUnit.SECONDS);

        assertTrue(
                ttl != null && ttl > 0);

        assertTrue(
                ttl <= Duration.ofMinutes(5)
                        .toSeconds());
    }

    @Test
    void shouldExpireToolResult() throws Exception {

        AiCacheProperties properties = properties(
                Duration.ofMinutes(5),
                Duration.ofSeconds(1),
                true);

        RedisCacheKeyMapper shortTtlKeyMapper = new RedisCacheKeyMapper(
                properties);

        RedisToolResultCache shortTtlCache = new RedisToolResultCache(
                redisTemplate,
                shortTtlKeyMapper,
                new StringRedisCacheSerializer(),
                namespaceCleaner,
                properties);

        waitUntilMissing(
                shortTtlKeyMapper.toolKey(
                        "expiring-tool-key"),
                Duration.ofSeconds(4));

        shortTtlCache.put(
                "expiring-tool-key",
                "result");

        assertTrue(
                shortTtlCache.get(
                        "expiring-tool-key")
                        .isPresent());

        waitUntilMissing(
                keyMapper.toolKey(
                        "expiring-tool-key"),
                Duration.ofSeconds(4));

        assertTrue(
                shortTtlCache.get(
                        "expiring-tool-key")
                        .isEmpty());
    }

    @Test
    void shouldRoundTripRetrievalResultThroughRealRedis() {

        Document first = document(
                "doc-1",
                "劳动合同解除",
                0.95,
                Map.of(
                        "tenantId",
                        "tenant-a",
                        "source",
                        "labor-law.pdf",
                        "version",
                        "v1"));

        Document second = document(
                "doc-2",
                "违法解除的赔偿责任",
                0.87,
                Map.of(
                        "tenantId",
                        "tenant-a",
                        "source",
                        "labor-contract-law.pdf",
                        "version",
                        "v1"));

        CachedRetrievalResult source = new CachedRetrievalResult(
                List.of(
                        first,
                        second));

        retrievalCache.put(
                "retrieval-key",
                source);

        CachedRetrievalResult restored = retrievalCache
                .get("retrieval-key")
                .orElseThrow();

        assertEquals(
                2,
                restored.documentCount());

        assertEquals(
                List.of(
                        "doc-1",
                        "doc-2"),
                restored.documents()
                        .stream()
                        .map(Document::getId)
                        .toList());

        assertEquals(
                "劳动合同解除",
                restored.documents()
                        .getFirst()
                        .getText());

        assertEquals(
                "tenant-a",
                restored.documents()
                        .getFirst()
                        .getMetadata()
                        .get("tenantId"));

        assertEquals(
                0.95,
                restored.documents()
                        .getFirst()
                        .getScore());
    }

    @Test
    void shouldTreatEmptyRetrievalResultAsHit() {

        retrievalCache.put(
                "empty-result",
                new CachedRetrievalResult(
                        List.of()));

        Optional<CachedRetrievalResult> result = retrievalCache.get(
                "empty-result");

        assertTrue(
                result.isPresent());

        assertFalse(
                result.orElseThrow()
                        .hasDocuments());
    }

    @Test
    void shouldInvalidateSingleKeysIndependently() {

        toolCache.put(
                "tool-1",
                "result-1");

        toolCache.put(
                "tool-2",
                "result-2");

        toolCache.invalidate(
                "tool-1");

        assertTrue(
                toolCache.get(
                        "tool-1")
                        .isEmpty());

        assertTrue(
                toolCache.get(
                        "tool-2")
                        .isPresent());
    }

    @Test
    void shouldClearOnlyRetrievalNamespace() {

        toolCache.put(
                "shared-key",
                "tool-result");

        retrievalCache.put(
                "shared-key",
                new CachedRetrievalResult(
                        List.of(
                                document(
                                        "doc-1",
                                        "content",
                                        0.9,
                                        Map.of()))));

        retrievalCache.clear();

        assertTrue(
                retrievalCache.get(
                        "shared-key")
                        .isEmpty());

        /*
         * Retrieval clear 不能删除 Tool Cache。
         */
        assertEquals(
                "tool-result",
                toolCache.get(
                        "shared-key")
                        .orElseThrow());
    }

    @Test
    void shouldClearOnlyToolNamespace() {

        toolCache.put(
                "shared-key",
                "tool-result");

        retrievalCache.put(
                "shared-key",
                new CachedRetrievalResult(
                        List.of(
                                document(
                                        "doc-1",
                                        "content",
                                        0.9,
                                        Map.of()))));

        toolCache.clear();

        assertTrue(
                toolCache.get(
                        "shared-key")
                        .isEmpty());

        /*
         * Tool clear 不能删除 Retrieval Cache。
         */
        assertTrue(
                retrievalCache.get(
                        "shared-key")
                        .isPresent());
    }

    @Test
    void shouldDeleteCorruptedRetrievalPayloadAndReturnMiss() {

        String redisKey = keyMapper.retrievalKey(
                "corrupted-key");

        redisTemplate
                .opsForValue()
                .set(
                        redisKey,
                        "{broken-json",
                        Duration.ofMinutes(5));

        assertTrue(
                retrievalCache.get(
                        "corrupted-key")
                        .isEmpty());

        /*
         * fail-open 模式下，坏数据应被删除。
         */
        assertFalse(
                Boolean.TRUE.equals(
                        redisTemplate.hasKey(
                                redisKey)));
    }

    @Test
    void shouldUseSeparateToolAndRetrievalKeys() {

        toolCache.put(
                "same-business-key",
                "tool-result");

        retrievalCache.put(
                "same-business-key",
                new CachedRetrievalResult(
                        List.of()));

        assertTrue(
                Boolean.TRUE.equals(
                        redisTemplate.hasKey(
                                keyMapper.toolKey(
                                        "same-business-key"))));

        assertTrue(
                Boolean.TRUE.equals(
                        redisTemplate.hasKey(
                                keyMapper.retrievalKey(
                                        "same-business-key"))));
    }

    @Test
    void shouldTreatUnavailableRedisAsMissWhenFailOpen() {

        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(
                "127.0.0.1",
                1);

        LettuceConnectionFactory unavailableFactory = new LettuceConnectionFactory(
                configuration);

        unavailableFactory.afterPropertiesSet();
        unavailableFactory.start();

        try {
            StringRedisTemplate unavailableTemplate = new StringRedisTemplate(
                    unavailableFactory);

            unavailableTemplate.afterPropertiesSet();

            AiCacheProperties properties = properties(
                    Duration.ofMinutes(5),
                    Duration.ofMinutes(5),
                    true);

            RedisToolResultCache unavailableCache = new RedisToolResultCache(
                    unavailableTemplate,
                    new RedisCacheKeyMapper(
                            properties),
                    new StringRedisCacheSerializer(),
                    new RedisNamespaceCleaner(
                            unavailableTemplate),
                    properties);

            assertTrue(
                    unavailableCache.get(
                            "unavailable-key")
                            .isEmpty());
        } finally {
            unavailableFactory.destroy();
        }
    }

    private AiCacheProperties properties(
            Duration retrievalTtl,
            Duration toolTtl,
            boolean failOpen) {

        AiCacheProperties properties = new AiCacheProperties();

        properties.getRedis()
                .setKeyPrefix(
                        KEY_PREFIX);

        properties.getRedis()
                .setFailOpen(
                        failOpen);

        properties.getRetrieval()
                .setTtl(
                        retrievalTtl);

        properties.getTool()
                .setTtl(
                        toolTtl);

        return properties;
    }

    private Document document(
            String id,
            String text,
            double score,
            Map<String, Object> metadata) {

        return Document.builder()
                .id(id)
                .text(text)
                .score(score)
                .metadata(metadata)
                .build();
    }

    private void waitUntilMissing(
            String redisKey,
            Duration timeout)
            throws InterruptedException {

        long deadline = System.nanoTime()
                + timeout.toNanos();

        while (System.nanoTime() < deadline) {

            if (!Boolean.TRUE.equals(
                    redisTemplate.hasKey(
                            redisKey))) {
                return;
            }

            Thread.sleep(50);
        }

        throw new AssertionError(
                "Redis key did not expire: "
                        + redisKey);
    }
}