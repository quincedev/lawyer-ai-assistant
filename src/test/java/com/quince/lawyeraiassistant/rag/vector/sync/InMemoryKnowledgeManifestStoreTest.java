package com.quince.lawyeraiassistant.rag.vector.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryKnowledgeManifestStoreTest {

    private InMemoryKnowledgeManifestStore store;

    @BeforeEach
    void setUp() {

        store = new InMemoryKnowledgeManifestStore();
    }

    @Test
    void shouldSaveAndFindManifestBySourceUri() {

        KnowledgeSourceManifest manifest = manifest(
                "classpath:knowledge/a.pdf",
                "checksum-a");

        store.save(
                manifest);

        KnowledgeSourceManifest restored = store.findBySourceUri(
                manifest.sourceUri())
                .orElseThrow();

        assertEquals(
                manifest,
                restored);
    }

    @Test
    void shouldNormalizeSourceUriWhenFinding() {

        KnowledgeSourceManifest manifest = manifest(
                "classpath:knowledge/a.pdf",
                "checksum-a");

        store.save(
                manifest);

        assertTrue(
                store.findBySourceUri(
                        "  classpath:knowledge/a.pdf  ")
                        .isPresent());
    }

    @Test
    void shouldReplaceManifestForSameSourceUri() {

        KnowledgeSourceManifest first = manifest(
                "classpath:knowledge/a.pdf",
                "checksum-v1");

        KnowledgeSourceManifest second = manifest(
                "classpath:knowledge/a.pdf",
                "checksum-v2");

        store.save(first);
        store.save(second);

        KnowledgeSourceManifest restored = store.findBySourceUri(
                "classpath:knowledge/a.pdf")
                .orElseThrow();

        assertEquals(
                "checksum-v2",
                restored.sourceChecksum());

        assertEquals(
                1,
                store.findAll().size());
    }

    @Test
    void shouldReturnAllManifests() {

        KnowledgeSourceManifest first = manifest(
                "classpath:knowledge/a.pdf",
                "checksum-a");

        KnowledgeSourceManifest second = manifest(
                "classpath:knowledge/b.pdf",
                "checksum-b");

        store.save(first);
        store.save(second);

        assertEquals(
                2,
                store.findAll().size());

        assertTrue(
                store.findAll()
                        .containsAll(
                                List.of(
                                        first,
                                        second)));
    }

    @Test
    void shouldDeleteManifestBySourceUri() {

        KnowledgeSourceManifest manifest = manifest(
                "classpath:knowledge/a.pdf",
                "checksum-a");

        store.save(
                manifest);

        store.deleteBySourceUri(
                manifest.sourceUri());

        assertTrue(
                store.findBySourceUri(
                        manifest.sourceUri())
                        .isEmpty());
    }

    @Test
    void shouldIgnoreInvalidFindAndDeleteArguments() {

        assertTrue(
                store.findBySourceUri(null)
                        .isEmpty());

        assertTrue(
                store.findBySourceUri(" ")
                        .isEmpty());

        store.deleteBySourceUri(null);
        store.deleteBySourceUri(" ");

        assertTrue(
                store.findAll()
                        .isEmpty());
    }

    private KnowledgeSourceManifest manifest(
            String sourceUri,
            String checksum) {

        String suffix = checksum.replace(
                "-",
                "");

        return new KnowledgeSourceManifest(
                "source:" + suffix,
                sourceUri,
                checksum,
                "v1",
                List.of(
                        "parent:" + suffix),
                List.of(
                        "chunk:" + suffix));
    }
}