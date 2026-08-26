package com.quince.lawyeraiassistant.rag.vector.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.quince.lawyeraiassistant.rag.vector.service.VectorSearchService;
import com.quince.lawyeraiassistant.retrieval.parent.provider.ParentDocumentStore;
import com.quince.lawyeraiassistant.retrieval.tenant.KnowledgeMetadata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.ai.document.Document;

class KnowledgeBaseSynchronizerTest {

        private KnowledgeManifestStore manifestStore;

        private ParentDocumentStore parentDocumentStore;

        private VectorSearchService vectorSearchService;

        private KnowledgeCacheInvalidator cacheInvalidator;

        private KnowledgeBaseSynchronizer synchronizer;

        @BeforeEach
        void setUp() {

                manifestStore = mock(KnowledgeManifestStore.class);

                parentDocumentStore = mock(ParentDocumentStore.class);

                vectorSearchService = mock(VectorSearchService.class);

                cacheInvalidator = mock(KnowledgeCacheInvalidator.class);

                synchronizer = new KnowledgeBaseSynchronizer(
                                manifestStore,
                                parentDocumentStore,
                                vectorSearchService,
                                cacheInvalidator);

                when(manifestStore.findAll())
                                .thenReturn(List.of());
        }

        @Test
        void shouldAddNewSource() {

                LoadedKnowledgeSource source = source(
                                "classpath:knowledge/a.pdf",
                                "checksum-v1",
                                "v1");

                when(manifestStore.findBySourceUri(
                                source.sourceUri()))
                                .thenReturn(Optional.empty());

                KnowledgeSyncResult result = synchronizer.synchronize(
                                batch(source));

                assertEquals(
                                1,
                                result.addedSources());

                assertEquals(
                                0,
                                result.unchangedSources());

                assertEquals(
                                1,
                                result.addedParents());

                assertEquals(
                                1,
                                result.addedChunks());

                verify(parentDocumentStore)
                                .saveAll(
                                                source.parents());

                verify(vectorSearchService)
                                .addDocuments(
                                                source.chunks());

                verify(manifestStore)
                                .save(
                                                KnowledgeSourceManifest.from(
                                                                source));

                verify(cacheInvalidator)
                                .invalidateAfter(
                                                result);
        }

        @Test
        void shouldSkipUnchangedSource() {

                LoadedKnowledgeSource source = source(
                                "classpath:knowledge/a.pdf",
                                "checksum-v1",
                                "v1");

                KnowledgeSourceManifest manifest = KnowledgeSourceManifest.from(
                                source);

                when(manifestStore.findBySourceUri(
                                source.sourceUri()))
                                .thenReturn(
                                                Optional.of(manifest));

                when(manifestStore.findAll())
                                .thenReturn(
                                                List.of(manifest));

                KnowledgeSyncResult result = synchronizer.synchronize(
                                batch(source));

                assertEquals(
                                1,
                                result.unchangedSources());

                verifyNoInteractions(
                                parentDocumentStore,
                                vectorSearchService);

                verify(manifestStore, never())
                                .save(
                                                manifest);

                verify(cacheInvalidator)
                                .invalidateAfter(
                                                result);
        }

        @Test
        void shouldReplaceChangedSource() {

                LoadedKnowledgeSource oldSource = source(
                                "classpath:knowledge/a.pdf",
                                "checksum-v1",
                                "v1");

                LoadedKnowledgeSource newSource = source(
                                "classpath:knowledge/a.pdf",
                                "checksum-v2",
                                "v1");

                KnowledgeSourceManifest oldManifest = KnowledgeSourceManifest.from(
                                oldSource);

                when(manifestStore.findBySourceUri(
                                newSource.sourceUri()))
                                .thenReturn(
                                                Optional.of(oldManifest));

                when(manifestStore.findAll())
                                .thenReturn(
                                                List.of(oldManifest));

                KnowledgeSyncResult result = synchronizer.synchronize(
                                batch(newSource));

                assertEquals(
                                1,
                                result.replacedSources());

                verify(parentDocumentStore)
                                .saveAll(
                                                newSource.parents());

                verify(vectorSearchService)
                                .addDocuments(
                                                newSource.chunks());

                verify(vectorSearchService)
                                .deleteDocuments(
                                                oldManifest.chunkIds());

                verify(parentDocumentStore)
                                .deleteByIds(
                                                oldManifest.parentIds());

                verify(manifestStore)
                                .save(
                                                KnowledgeSourceManifest.from(
                                                                newSource));

                verify(cacheInvalidator)
                                .invalidateAfter(
                                                result);
        }

        @Test
        void shouldRemoveMissingSource() {

                LoadedKnowledgeSource oldSource = source(
                                "classpath:knowledge/removed.pdf",
                                "checksum-v1",
                                "v1");

                KnowledgeSourceManifest oldManifest = KnowledgeSourceManifest.from(
                                oldSource);

                when(manifestStore.findAll())
                                .thenReturn(
                                                List.of(oldManifest));

                KnowledgeSyncResult result = synchronizer.synchronize(
                                new LoadedKnowledgeBatch(
                                                List.of()));

                assertEquals(
                                1,
                                result.removedSources());

                verify(vectorSearchService)
                                .deleteDocuments(
                                                oldManifest.chunkIds());

                verify(parentDocumentStore)
                                .deleteByIds(
                                                oldManifest.parentIds());

                verify(manifestStore)
                                .deleteBySourceUri(
                                                oldManifest.sourceUri());

                verify(cacheInvalidator)
                                .invalidateAfter(
                                                result);
        }

        @Test
        void shouldCommitManifestAfterDocumentsAreStored() {

                LoadedKnowledgeSource source = source(
                                "classpath:knowledge/a.pdf",
                                "checksum-v1",
                                "v1");

                when(manifestStore.findBySourceUri(
                                source.sourceUri()))
                                .thenReturn(Optional.empty());

                synchronizer.synchronize(
                                batch(source));

                InOrder order = inOrder(
                                parentDocumentStore,
                                vectorSearchService,
                                manifestStore);

                order.verify(parentDocumentStore)
                                .saveAll(
                                                source.parents());

                order.verify(vectorSearchService)
                                .addDocuments(
                                                source.chunks());

                order.verify(manifestStore)
                                .save(
                                                KnowledgeSourceManifest.from(
                                                                source));
        }

        @Test
        void shouldNotCommitManifestWhenVectorWriteFails() {

                LoadedKnowledgeSource source = source(
                                "classpath:knowledge/a.pdf",
                                "checksum-v1",
                                "v1");

                when(manifestStore.findBySourceUri(
                                source.sourceUri()))
                                .thenReturn(Optional.empty());

                when(vectorSearchService.addDocuments(
                                source.chunks()))
                                .thenThrow(
                                                new IllegalStateException(
                                                                "vector failure"));

                assertThrows(
                                IllegalStateException.class,
                                () -> synchronizer.synchronize(
                                                batch(source)));

                verify(manifestStore, never())
                                .save(
                                                KnowledgeSourceManifest.from(
                                                                source));

                verifyNoInteractions(
                                cacheInvalidator);
        }

        private LoadedKnowledgeBatch batch(
                        LoadedKnowledgeSource source) {

                return new LoadedKnowledgeBatch(
                                List.of(source));
        }

        private LoadedKnowledgeSource source(
                        String sourceUri,
                        String sourceChecksum,
                        String version) {

                String suffix = sourceChecksum.replace(
                                "-",
                                "");

                String sourceId = "source:" + suffix;

                Document parent = Document.builder()
                                .id("parent:" + suffix)
                                .text("Parent " + sourceChecksum)
                                .metadata(
                                                Map.of(
                                                                KnowledgeMetadata.SOURCE_DOCUMENT_ID,
                                                                sourceId,
                                                                KnowledgeMetadata.SOURCE_URI,
                                                                sourceUri,
                                                                KnowledgeMetadata.SOURCE_CHECKSUM,
                                                                sourceChecksum,
                                                                KnowledgeMetadata.CONTENT_CHECKSUM,
                                                                "parent-" + sourceChecksum,
                                                                KnowledgeMetadata.KNOWLEDGE_VERSION,
                                                                version))
                                .build();

                Document chunk = Document.builder()
                                .id("chunk:" + suffix)
                                .text("Chunk " + sourceChecksum)
                                .metadata(
                                                Map.of(
                                                                KnowledgeMetadata.SOURCE_DOCUMENT_ID,
                                                                sourceId,
                                                                KnowledgeMetadata.SOURCE_URI,
                                                                sourceUri,
                                                                KnowledgeMetadata.SOURCE_CHECKSUM,
                                                                sourceChecksum,
                                                                KnowledgeMetadata.PARENT_DOCUMENT_ID,
                                                                parent.getId(),
                                                                KnowledgeMetadata.CONTENT_CHECKSUM,
                                                                "chunk-" + sourceChecksum,
                                                                KnowledgeMetadata.KNOWLEDGE_VERSION,
                                                                version))
                                .build();

                return new LoadedKnowledgeSource(
                                sourceId,
                                "test.pdf",
                                sourceUri,
                                sourceChecksum,
                                version,
                                List.of(parent),
                                List.of(chunk));
        }
}