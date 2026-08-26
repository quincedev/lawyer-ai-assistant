package com.quince.lawyeraiassistant.rag.vector.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;

import com.quince.lawyeraiassistant.common.exception.KnowledgeBaseException;
import com.quince.lawyeraiassistant.retrieval.config.RetrievalProperties;
import com.quince.lawyeraiassistant.retrieval.tenant.TenantKnowledgeFilterFactory;
import org.springframework.ai.document.Document;

public class VectorSearchServiceTest {

        private VectorStore vectorStore;

        private VectorSearchService service;

        @BeforeEach
        void setUp() {

                vectorStore = mock(VectorStore.class);

                RetrievalProperties retrievalProperties = new RetrievalProperties(
                                5,
                                0.7);

                TenantKnowledgeFilterFactory filterFactory = mock(
                                TenantKnowledgeFilterFactory.class);

                service = new VectorSearchService(
                                vectorStore,
                                retrievalProperties,
                                filterFactory);
        }

        @Test
        void shouldIgnoreNullAndEmptyDocumentLists() {

                assertEquals(0, service.addDocuments(null));
                assertEquals(0, service.addDocuments(List.of()));
                verifyNoInteractions(vectorStore);
        }

        @Test
        void shouldWriteOnlyOneDocumentForDuplicateId() {

                Document first = Document.builder()
                                .id("chunk:test")
                                .text("相同正文")
                                .metadata(Map.of())
                                .build();

                Document second = Document.builder()
                                .id("chunk:test")
                                .text("相同正文")
                                .metadata(Map.of())
                                .build();

                int result = service.addDocuments(
                                List.of(
                                                first,
                                                second));

                assertEquals(
                                1,
                                result);

                verify(vectorStore)
                                .add(
                                                List.of(first));
        }

        @Test
        void shouldRejectSameIdWithDifferentContent() {

                Document first = Document.builder()
                                .id("chunk:test")
                                .text("第一份正文")
                                .metadata(Map.of())
                                .build();

                Document second = Document.builder()
                                .id("chunk:test")
                                .text("第二份正文")
                                .metadata(Map.of())
                                .build();

                assertThrows(
                                KnowledgeBaseException.class,
                                () -> service.addDocuments(
                                                List.of(
                                                                first,
                                                                second)));

                verifyNoInteractions(
                                vectorStore);
        }

        @Test
        void shouldDeleteDistinctVectorDocumentIds() {

                int deleted = service.deleteDocuments(
                                List.of(
                                                "chunk:1",
                                                "chunk:1",
                                                "chunk:2",
                                                " "));

                assertEquals(
                                2,
                                deleted);

                verify(vectorStore)
                                .delete(
                                                List.of(
                                                                "chunk:1",
                                                                "chunk:2"));
        }

        @Test
        void shouldIgnoreEmptyVectorDeleteRequest() {

                assertEquals(
                                0,
                                service.deleteDocuments(
                                                List.of()));

                assertEquals(
                                0,
                                service.deleteDocuments(
                                                null));

                verifyNoInteractions(
                                vectorStore);
        }

}
