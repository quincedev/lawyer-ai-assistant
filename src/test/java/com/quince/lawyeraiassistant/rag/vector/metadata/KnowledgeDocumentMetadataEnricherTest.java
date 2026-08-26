package com.quince.lawyeraiassistant.rag.vector.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import com.quince.lawyeraiassistant.rag.vector.config.KnowledgeBaseProperties;
import com.quince.lawyeraiassistant.rag.vector.identity.KnowledgeDocumentIdentityFactory;
import com.quince.lawyeraiassistant.retrieval.tenant.KnowledgeMetadata;
import com.quince.lawyeraiassistant.retrieval.tenant.KnowledgeScope;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

class KnowledgeDocumentMetadataEnricherTest {

        private static final String SOURCE_CHECKSUM = "0123456789abcdef0123456789abcdef"
                        + "0123456789abcdef0123456789abcdef";

        private KnowledgeDocumentMetadataEnricher enricher;

        @BeforeEach
        void setUp() {

                enricher = new KnowledgeDocumentMetadataEnricher(
                                new KnowledgeDocumentIdentityFactory(),
                                new KnowledgeBaseProperties(
                                                true,
                                                "classpath:knowledge/",
                                                "v1"));
        }

        @Test
        void shouldEnrichSharedParent() {

                Document source = Document.builder()
                                .id("unstable-reader-id")
                                .text("第一页法律正文")
                                .metadata(
                                                Map.of(
                                                                KnowledgeMetadata.TENANT_ID,
                                                                "should-be-removed"))
                                .build();

                Document result = enricher.enrichParent(
                                source,
                                "labor-law.pdf",
                                "classpath:knowledge/labor-law.pdf",
                                SOURCE_CHECKSUM,
                                1);

                assertTrue(
                                result.getId()
                                                .startsWith("parent:"));

                assertEquals(
                                result.getId(),
                                result.getMetadata()
                                                .get(
                                                                KnowledgeMetadata.PARENT_DOCUMENT_ID));

                assertEquals(
                                "labor-law.pdf",
                                result.getMetadata()
                                                .get(
                                                                KnowledgeMetadata.SOURCE_NAME));

                assertEquals(
                                "classpath:knowledge/labor-law.pdf",
                                result.getMetadata()
                                                .get(
                                                                KnowledgeMetadata.SOURCE_URI));

                assertEquals(
                                SOURCE_CHECKSUM,
                                result.getMetadata()
                                                .get(
                                                                KnowledgeMetadata.SOURCE_CHECKSUM));

                assertEquals(
                                "v1",
                                result.getMetadata()
                                                .get(
                                                                KnowledgeMetadata.KNOWLEDGE_VERSION));

                assertEquals(
                                KnowledgeScope.SHARED.name(),
                                result.getMetadata()
                                                .get(
                                                                KnowledgeMetadata.KNOWLEDGE_SCOPE));

                assertFalse(
                                result.getMetadata()
                                                .containsKey(
                                                                KnowledgeMetadata.TENANT_ID));

                String contentChecksum = result.getMetadata()
                                .get(
                                                KnowledgeMetadata.CONTENT_CHECKSUM)
                                .toString();

                assertEquals(
                                64,
                                contentChecksum.length());
        }

        @Test
        void shouldGenerateDeterministicParentIdentity() {

                Document first = enricher.enrichParent(
                                sourceDocument(
                                                "random-reader-id-1",
                                                "相同法律正文"),
                                "labor-law.pdf",
                                "classpath:knowledge/labor-law.pdf",
                                SOURCE_CHECKSUM,
                                1);

                Document second = enricher.enrichParent(
                                sourceDocument(
                                                "random-reader-id-2",
                                                "相同法律正文"),
                                "labor-law.pdf",
                                "classpath:knowledge/labor-law.pdf",
                                SOURCE_CHECKSUM,
                                1);

                assertEquals(
                                first.getId(),
                                second.getId());

                assertEquals(
                                first.getMetadata()
                                                .get(
                                                                KnowledgeMetadata.CONTENT_CHECKSUM),
                                second.getMetadata()
                                                .get(
                                                                KnowledgeMetadata.CONTENT_CHECKSUM));
        }

        @Test
        void shouldChangeParentIdentityWhenContentChanges() {

                Document first = enricher.enrichParent(
                                sourceDocument(
                                                "reader-id",
                                                "旧法律正文"),
                                "labor-law.pdf",
                                "classpath:knowledge/labor-law.pdf",
                                SOURCE_CHECKSUM,
                                1);

                Document second = enricher.enrichParent(
                                sourceDocument(
                                                "reader-id",
                                                "新法律正文"),
                                "labor-law.pdf",
                                "classpath:knowledge/labor-law.pdf",
                                SOURCE_CHECKSUM,
                                1);

                assertNotEquals(
                                first.getId(),
                                second.getId());
        }

        @Test
        void shouldEnrichChunkAndPreserveSourceChecksum() {

                Document parent = enricher.enrichParent(
                                sourceDocument(
                                                "reader-id",
                                                "完整父文档正文"),
                                "labor-law.pdf",
                                "classpath:knowledge/labor-law.pdf",
                                SOURCE_CHECKSUM,
                                1);

                String parentContentChecksum = parent.getMetadata()
                                .get(
                                                KnowledgeMetadata.CONTENT_CHECKSUM)
                                .toString();

                Document rawChunk = sourceDocument(
                                "unstable-chunk-id",
                                "Chunk 正文");

                Document chunk = enricher.enrichChunk(
                                rawChunk,
                                parent,
                                0);

                assertTrue(
                                chunk.getId()
                                                .startsWith("chunk:"));

                assertEquals(
                                chunk.getId(),
                                chunk.getMetadata()
                                                .get(
                                                                KnowledgeMetadata.CHUNK_ID));

                assertEquals(
                                parent.getId(),
                                chunk.getMetadata()
                                                .get(
                                                                KnowledgeMetadata.PARENT_DOCUMENT_ID));

                assertEquals(
                                0,
                                chunk.getMetadata()
                                                .get(
                                                                KnowledgeMetadata.CHUNK_INDEX));

                assertEquals(
                                SOURCE_CHECKSUM,
                                chunk.getMetadata()
                                                .get(
                                                                KnowledgeMetadata.SOURCE_CHECKSUM));

                String chunkContentChecksum = chunk.getMetadata()
                                .get(
                                                KnowledgeMetadata.CONTENT_CHECKSUM)
                                .toString();

                assertEquals(
                                64,
                                chunkContentChecksum.length());

                assertNotEquals(
                                parentContentChecksum,
                                chunkContentChecksum);
        }

        private Document sourceDocument(
                        String id,
                        String text) {

                return Document.builder()
                                .id(id)
                                .text(text)
                                .metadata(Map.of())
                                .build();
        }
}