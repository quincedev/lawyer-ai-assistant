package com.quince.lawyeraiassistant.retrieval.parent.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryParentDocumentProviderTest {

        private InMemoryParentDocumentProvider provider;

        @BeforeEach
        void setUp() {
                provider = new InMemoryParentDocumentProvider();
        }

        @Test
        void shouldSaveAndFindDocumentByItsOwnId() {
                Document document = createDocument(
                                "parent-1",
                                "劳动合同法第四十六条");

                provider.save(document);

                Optional<Document> result = provider.findById(
                                "parent-1");

                assertTrue(result.isPresent());
                assertSame(document, result.get());
                assertEquals(1, provider.size());
        }

        @Test
        void shouldSaveAllDocuments() {
                Document first = createDocument(
                                "parent-1",
                                "第一页");

                Document second = createDocument(
                                "parent-2",
                                "第二页");

                provider.saveAll(
                                List.of(first, second));

                assertEquals(2, provider.size());
                assertSame(
                                first,
                                provider.findById("parent-1")
                                                .orElseThrow());
                assertSame(
                                second,
                                provider.findById("parent-2")
                                                .orElseThrow());
        }

        @Test
        void shouldOverwriteExistingDocumentWithSameId() {
                Document original = createDocument(
                                "parent-1",
                                "旧内容");

                Document updated = createDocument(
                                "parent-1",
                                "新内容");

                provider.save(original);
                provider.save(updated);

                assertEquals(1, provider.size());

                assertSame(
                                updated,
                                provider.findById("parent-1")
                                                .orElseThrow());
        }

        @Test
        void shouldFindAllInRequestedOrderAndIgnoreDuplicates() {
                Document first = createDocument(
                                "parent-1",
                                "A");

                Document second = createDocument(
                                "parent-2",
                                "B");

                provider.saveAll(
                                List.of(first, second));

                Collection<Document> result = provider.findAllByIds(
                                List.of(
                                                "parent-2",
                                                "parent-1",
                                                "parent-2",
                                                "missing"));

                assertEquals(
                                List.of(second, first),
                                List.copyOf(result));
        }

        @Test
        void shouldIgnoreNullAndBlankIdsWhenFindingAll() {
                Document document = createDocument(
                                "parent-1",
                                "A");

                provider.save(document);

                List<String> ids = new ArrayList<>();

                ids.add(null);
                ids.add(" ");
                ids.add("parent-1");

                Collection<Document> result = provider.findAllByIds(ids);

                assertEquals(
                                List.of(document),
                                List.copyOf(result));
        }

        @Test
        void shouldReturnEmptyWhenIdDoesNotExist() {
                assertTrue(
                                provider.findById("missing")
                                                .isEmpty());

                assertTrue(
                                provider.findById(null)
                                                .isEmpty());

                assertTrue(
                                provider.findById(" ")
                                                .isEmpty());
        }

        @Test
        void shouldRejectNullDocument() {
                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> provider.save(null));

                assertEquals(
                                "document must not be null",
                                exception.getMessage());
        }

        @Test
        void shouldClearAllDocuments() {
                provider.save(
                                createDocument(
                                                "parent-1",
                                                "A"));

                provider.clear();

                assertEquals(0, provider.size());
        }

        @Test
        void shouldDeleteOnlyRequestedParents() {

                Document first = Document.builder()
                                .id("parent:1")
                                .text("first")
                                .metadata(Map.of())
                                .build();

                Document second = Document.builder()
                                .id("parent:2")
                                .text("second")
                                .metadata(Map.of())
                                .build();

                provider.saveAll(
                                List.of(
                                                first,
                                                second));

                provider.deleteByIds(
                                java.util.Arrays.asList(
                                                "parent:1",
                                                "parent:1",
                                                null,
                                                " "));

                assertTrue(
                                provider.findById(
                                                "parent:1")
                                                .isEmpty());

                assertTrue(
                                provider.findById(
                                                "parent:2")
                                                .isPresent());

                assertEquals(
                                1,
                                provider.size());
        }

        private Document createDocument(
                        String id,
                        String text) {
                return new Document(
                                id,
                                text,
                                Map.of());
        }
}