package com.quince.lawyeraiassistant.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

class CacheKeyFactoryTest {

    private CacheKeyFactory factory;

    @BeforeEach
    void setUp() {

        factory = new CacheKeyFactory(
                new ObjectMapper());
    }

    @Test
    void shouldGenerateSameRetrievalKeyForSameInput() {

        String first = factory.retrievalKey(
                CacheScope.TENANT,
                "tenant-a",
                "劳动合同解除",
                "v1",
                5,
                0.0);

        String second = factory.retrievalKey(
                CacheScope.TENANT,
                "tenant-a",
                "劳动合同解除",
                "v1",
                5,
                0.0);

        assertEquals(
                first,
                second);
    }

    @Test
    void shouldIsolateRetrievalCacheByTenant() {

        String tenantA = factory.retrievalKey(
                CacheScope.TENANT,
                "tenant-a",
                "劳动合同解除",
                "v1",
                5,
                0.0);

        String tenantB = factory.retrievalKey(
                CacheScope.TENANT,
                "tenant-b",
                "劳动合同解除",
                "v1",
                5,
                0.0);

        assertNotEquals(
                tenantA,
                tenantB);
    }

    @Test
    void shouldIgnoreTenantIdForSharedScope() {

        String first = factory.retrievalKey(
                CacheScope.SHARED,
                "tenant-a",
                "劳动合同解除",
                "v1",
                5,
                0.0);

        String second = factory.retrievalKey(
                CacheScope.SHARED,
                "tenant-b",
                "劳动合同解除",
                "v1",
                5,
                0.0);

        assertEquals(
                first,
                second);
    }

    @Test
    void shouldChangeKeyWhenKnowledgeVersionChanges() {

        String v1 = factory.retrievalKey(
                CacheScope.SHARED,
                null,
                "劳动合同解除",
                "v1",
                5,
                0.0);

        String v2 = factory.retrievalKey(
                CacheScope.SHARED,
                null,
                "劳动合同解除",
                "v2",
                5,
                0.0);

        assertNotEquals(
                v1,
                v2);
    }

    @Test
    void shouldChangeKeyWhenTopKChanges() {

        String top5 = factory.retrievalKey(
                CacheScope.SHARED,
                null,
                "劳动合同解除",
                "v1",
                5,
                0.0);

        String top10 = factory.retrievalKey(
                CacheScope.SHARED,
                null,
                "劳动合同解除",
                "v1",
                10,
                0.0);

        assertNotEquals(
                top5,
                top10);
    }

    @Test
    void shouldGenerateSameToolKeyRegardlessOfMapOrder() {

        Map<String, Object> firstArguments = new LinkedHashMap<>();

        firstArguments.put(
                "legalQuestion",
                "劳动合同解除");

        firstArguments.put(
                "language",
                "zh");

        Map<String, Object> secondArguments = new LinkedHashMap<>();

        secondArguments.put(
                "language",
                "zh");

        secondArguments.put(
                "legalQuestion",
                "劳动合同解除");

        String first = factory.toolKey(
                CacheScope.TENANT,
                "tenant-a",
                "searchLegalKnowledge",
                firstArguments,
                "v1");

        String second = factory.toolKey(
                CacheScope.TENANT,
                "tenant-a",
                "searchLegalKnowledge",
                secondArguments,
                "v1");

        assertEquals(
                first,
                second);
    }

    @Test
    void shouldNotCollideWhenRetrievalFieldsContainSeparator() {

        String first = factory.retrievalKey(
                CacheScope.SHARED, null, "a|b", "c", 5, 0.0);
        String second = factory.retrievalKey(
                CacheScope.SHARED, null, "a", "b|c", 5, 0.0);

        assertNotEquals(first, second);
    }

    @Test
    void shouldChangeRetrievalKeyWhenSimilarityThresholdChanges() {

        String first = factory.retrievalKey(
                CacheScope.SHARED, null, "query", "v1", 5, 0.1);
        String second = factory.retrievalKey(
                CacheScope.SHARED, null, "query", "v1", 5, 0.2);

        assertNotEquals(first, second);
    }

    @Test
    void shouldCanonicalizeNestedToolArgumentMapsButPreserveListOrder() {

        String first = factory.toolKey(
                CacheScope.SHARED,
                null,
                "searchLegalKnowledge",
                Map.of("filter", new LinkedHashMap<>(Map.of("b", 2, "a", 1)),
                        "terms", java.util.List.of("a", "b")),
                "v1");
        String reorderedMap = factory.toolKey(
                CacheScope.SHARED,
                null,
                "searchLegalKnowledge",
                Map.of("terms", java.util.List.of("a", "b"),
                        "filter", new LinkedHashMap<>(Map.of("a", 1, "b", 2))),
                "v1");
        String reorderedList = factory.toolKey(
                CacheScope.SHARED,
                null,
                "searchLegalKnowledge",
                Map.of("filter", Map.of("a", 1, "b", 2),
                        "terms", java.util.List.of("b", "a")),
                "v1");

        assertEquals(first, reorderedMap);
        assertNotEquals(first, reorderedList);
    }

    @Test
    void shouldApplyScopeAndVersionToToolKeys() {

        String tenantA = factory.toolKey(
                CacheScope.TENANT, "tenant-a", "searchLegalKnowledge", Map.of(), "v1");
        String tenantB = factory.toolKey(
                CacheScope.TENANT, "tenant-b", "searchLegalKnowledge", Map.of(), "v1");
        String sharedA = factory.toolKey(
                CacheScope.SHARED, "tenant-a", "searchLegalKnowledge", Map.of(), "v1");
        String sharedB = factory.toolKey(
                CacheScope.SHARED, "tenant-b", "searchLegalKnowledge", Map.of(), "v1");
        String v2 = factory.toolKey(
                CacheScope.SHARED, null, "searchLegalKnowledge", Map.of(), "v2");

        assertNotEquals(tenantA, tenantB);
        assertEquals(sharedA, sharedB);
        assertNotEquals(sharedA, v2);
    }

    @Test
    void shouldTreatNullToolArgumentsAsEmptyMap() {

        assertEquals(
                factory.toolKey(CacheScope.SHARED, null, "tool", null, "v1"),
                factory.toolKey(CacheScope.SHARED, null, "tool", Map.of(), "v1"));
    }

    @Test
    void shouldNormalizeRequiredTextAndRejectInvalidValues() {

        assertEquals(
                factory.retrievalKey(CacheScope.TENANT, " tenant ", " query ", " v1 ", 5, 0.0),
                factory.retrievalKey(CacheScope.TENANT, "tenant", "query", "v1", 5, 0.0));
        assertThrows(NullPointerException.class,
                () -> factory.retrievalKey(null, "tenant", "query", "v1", 5, 0.0));
        assertThrows(NullPointerException.class,
                () -> factory.retrievalKey(CacheScope.TENANT, null, "query", "v1", 5, 0.0));
        assertThrows(IllegalArgumentException.class,
                () -> factory.retrievalKey(CacheScope.SHARED, null, " ", "v1", 5, 0.0));
        assertThrows(IllegalArgumentException.class,
                () -> factory.toolKey(CacheScope.SHARED, null, " ", Map.of(), "v1"));
    }

    @Test
    void shouldReturnVersionedSha256Keys() {

        String retrieval = factory.retrievalKey(
                CacheScope.SHARED, null, "query", "v1", 5, 0.0);
        String tool = factory.toolKey(
                CacheScope.SHARED, null, "tool", Map.of(), "v1");

        assertTrue(retrieval.matches("rag:v1:[0-9a-f]{64}"));
        assertTrue(tool.matches("tool:v1:[0-9a-f]{64}"));
    }
}
