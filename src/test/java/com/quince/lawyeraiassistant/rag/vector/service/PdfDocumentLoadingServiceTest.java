package com.quince.lawyeraiassistant.rag.vector.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import com.quince.lawyeraiassistant.rag.splitter.LegalTextSplitter;
import com.quince.lawyeraiassistant.rag.vector.config.KnowledgeBaseProperties;
import com.quince.lawyeraiassistant.rag.vector.identity.KnowledgeDocumentIdentityFactory;
import com.quince.lawyeraiassistant.rag.vector.metadata.KnowledgeDocumentMetadataEnricher;
import com.quince.lawyeraiassistant.retrieval.tenant.KnowledgeMetadata;
import com.quince.lawyeraiassistant.retrieval.tenant.KnowledgeScope;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

class PdfDocumentLoadingServiceTest {

        private LegalTextSplitter legalTextSplitter;

        private PdfDocumentLoadingService service;

        @BeforeEach
        void setUp() {

                legalTextSplitter = mock(LegalTextSplitter.class);

                KnowledgeDocumentIdentityFactory identityFactory = new KnowledgeDocumentIdentityFactory();

                KnowledgeBaseProperties properties = new KnowledgeBaseProperties(
                                true,
                                "classpath:knowledge/",
                                "v1");

                KnowledgeDocumentMetadataEnricher enricher = new KnowledgeDocumentMetadataEnricher(
                                identityFactory,
                                properties);

                service = new PdfDocumentLoadingService(
                                legalTextSplitter,
                                enricher,
                                identityFactory,
                                properties);
        }

        @Test
        void shouldGenerateDeterministicChunkIdentity() {

                Document parent = parentDocument();

                Document rawChunk = Document.builder()
                                .id("random-id")
                                .text("Chunk 正文")
                                .metadata(Map.of())
                                .build();

                when(legalTextSplitter.split(
                                List.of(parent)))
                                .thenReturn(
                                                List.of(rawChunk));

                List<Document> first = service.splitAndEnrich(
                                parent);

                List<Document> second = service.splitAndEnrich(
                                parent);

                assertEquals(
                                first.getFirst().getId(),
                                second.getFirst().getId());

                assertEquals(
                                parent.getId(),
                                first.getFirst()
                                                .getMetadata()
                                                .get(
                                                                KnowledgeMetadata.PARENT_DOCUMENT_ID));

                assertEquals(
                                parent.getMetadata()
                                                .get(
                                                                KnowledgeMetadata.SOURCE_CHECKSUM),
                                first.getFirst()
                                                .getMetadata()
                                                .get(
                                                                KnowledgeMetadata.SOURCE_CHECKSUM));
        }

        @Test
        void shouldUseOnlyValidChunksForChunkIndex() {

                Document parent = parentDocument();

                Document blankChunk = Document.builder()
                                .id("blank")
                                .text(" ")
                                .metadata(Map.of())
                                .build();

                Document firstValid = Document.builder()
                                .id("first")
                                .text("第一段")
                                .metadata(Map.of())
                                .build();

                Document secondValid = Document.builder()
                                .id("second")
                                .text("第二段")
                                .metadata(Map.of())
                                .build();

                when(legalTextSplitter.split(
                                List.of(parent)))
                                .thenReturn(
                                                List.of(
                                                                blankChunk,
                                                                firstValid,
                                                                secondValid));

                List<Document> chunks = service.splitAndEnrich(
                                parent);

                assertEquals(
                                2,
                                chunks.size());

                assertEquals(
                                0,
                                chunks.get(0)
                                                .getMetadata()
                                                .get(
                                                                KnowledgeMetadata.CHUNK_INDEX));

                assertEquals(
                                1,
                                chunks.get(1)
                                                .getMetadata()
                                                .get(
                                                                KnowledgeMetadata.CHUNK_INDEX));

                assertNotEquals(
                                chunks.get(0).getId(),
                                chunks.get(1).getId());
        }

        @Test
        void shouldRemoveDuplicateChunksByStableId() {

                Document parent = parentDocument();

                Document first = Document.builder()
                                .id("reader-chunk-1")
                                .text("完全相同的 Chunk")
                                .metadata(Map.of())
                                .build();

                Document second = Document.builder()
                                .id("reader-chunk-2")
                                .text("完全相同的 Chunk")
                                .metadata(Map.of())
                                .build();

                when(legalTextSplitter.split(
                                List.of(parent)))
                                .thenReturn(
                                                List.of(
                                                                first,
                                                                second));

                List<Document> chunks = service.splitAndEnrich(parent);

                /*
                 * 注意：chunk_id 包含 chunk_index，
                 * 所以同一 Parent 中两个不同位置的相同正文
                 * 仍是两个合法 Chunk。
                 */
                assertEquals(
                                2,
                                chunks.size());

                assertNotEquals(
                                chunks.get(0).getId(),
                                chunks.get(1).getId());
        }

        private Document parentDocument() {

                return Document.builder()
                                .id("parent:test")
                                .text("完整正文")
                                .metadata(
                                                Map.of(
                                                                KnowledgeMetadata.SOURCE_CHECKSUM,
                                                                "source-checksum",
                                                                KnowledgeMetadata.CONTENT_CHECKSUM,
                                                                "parent-content-checksum",
                                                                KnowledgeMetadata.KNOWLEDGE_SCOPE,
                                                                KnowledgeScope.SHARED.name()))
                                .build();
        }
}
