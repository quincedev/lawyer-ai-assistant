package com.quince.lawyeraiassistant.cache.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.quince.lawyeraiassistant.cache.retrieval.CachedRetrievalResult;
import com.quince.lawyeraiassistant.retrieval.tenant.KnowledgeMetadata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

class CachedRetrievalResultRedisSerializerTest {

        private CachedRetrievalResultRedisSerializer serializer;

        @BeforeEach
        void setUp() {

                serializer = new CachedRetrievalResultRedisSerializer(
                                new ObjectMapper(),
                                new RedisDocumentMapper(
                                                new RedisMetadataValidator()));
        }

        @Test
        void shouldRoundTripCachedRetrievalResult() {

                Document source = document(
                                "doc-1",
                                "first document",
                                0.91,
                                Map.of());

                CachedRetrievalResult result = roundTrip(
                                new CachedRetrievalResult(
                                                List.of(source)));

                assertEquals(
                                1,
                                result.documentCount());

                assertDocumentEquals(
                                source,
                                result.documents().getFirst());
        }

        @Test
        void shouldPreserveDocumentOrderAndMetadata() {

                Map<String, Object> metadata = new LinkedHashMap<>();

                metadata.put(
                                "source",
                                "civil-code.pdf");

                metadata.put(
                                "page",
                                42);

                metadata.put(
                                "verified",
                                true);

                metadata.put(
                                "tags",
                                List.of(
                                                "contract",
                                                "termination"));

                metadata.put(
                                "location",
                                Map.of(
                                                "chapter",
                                                3,
                                                "section",
                                                "II"));

                CachedRetrievalResult result = roundTrip(
                                new CachedRetrievalResult(
                                                List.of(
                                                                document(
                                                                                "doc-1",
                                                                                "first",
                                                                                0.91,
                                                                                metadata),
                                                                document(
                                                                                "doc-2",
                                                                                "second",
                                                                                0.82,
                                                                                Map.of()))));

                assertEquals(
                                List.of(
                                                "doc-1",
                                                "doc-2"),
                                result.documents()
                                                .stream()
                                                .map(Document::getId)
                                                .toList());

                assertEquals(
                                metadata,
                                result.documents()
                                                .getFirst()
                                                .getMetadata());
        }

        @Test
        void shouldRoundTripEmptyResult() {

                CachedRetrievalResult result = roundTrip(
                                new CachedRetrievalResult(
                                                List.of()));

                assertFalse(
                                result.hasDocuments());

                assertEquals(
                                0,
                                result.documentCount());
        }

        @Test
        void shouldRejectNullInput() {

                assertThrows(
                                IllegalArgumentException.class,
                                () -> serializer.serialize(null));
        }

        @Test
        void shouldRejectBlankOrInvalidJson() {

                assertThrows(
                                RedisCacheSerializationException.class,
                                () -> serializer.deserialize(
                                                "  \r\n\t"));

                RedisCacheSerializationException exception = assertThrows(
                                RedisCacheSerializationException.class,
                                () -> serializer.deserialize(
                                                "[{not-valid-json}]"));

                assertTrue(
                                exception.getCause() instanceof JacksonException);
        }

        @Test
        void shouldRejectUnsupportedMetadataType() {

                CachedRetrievalResult result = new CachedRetrievalResult(
                                List.of(
                                                document(
                                                                "doc-1",
                                                                "content",
                                                                0.5,
                                                                Map.of(
                                                                                "unsupported",
                                                                                new StringBuilder(
                                                                                                "value")))));

                assertThrows(
                                RedisCacheSerializationException.class,
                                () -> serializer.serialize(result));
        }

        @Test
        void shouldNotIncludeJavaTypeInformationInJson() {

                String json = serializer.serialize(
                                new CachedRetrievalResult(
                                                List.of(
                                                                document(
                                                                                "doc-1",
                                                                                "content",
                                                                                0.9,
                                                                                Map.of(
                                                                                                "source",
                                                                                                "test")))));

                assertFalse(
                                json.contains("@class"));

                assertFalse(
                                json.contains("@type"));

                assertFalse(
                                json.contains(
                                                Document.class.getName()));

                assertFalse(
                                json.contains(
                                                RedisDocumentValue.class.getName()));

                assertTrue(
                                json.startsWith(
                                                "{\"documents\":[{"));
        }

        @Test
        void shouldPreserveChecksumMetadataAfterRoundTrip() {

                Map<String, Object> metadata = Map.of(
                                KnowledgeMetadata.SOURCE_CHECKSUM,
                                "source-checksum",
                                KnowledgeMetadata.CONTENT_CHECKSUM,
                                "content-checksum");

                Document source = document(
                                "parent:test",
                                "test content",
                                0.91,
                                metadata);

                CachedRetrievalResult result = roundTrip(
                                new CachedRetrievalResult(
                                                List.of(source)));

                Document restored = result.documents()
                                .getFirst();

                assertEquals(
                                "source-checksum",
                                restored.getMetadata()
                                                .get(
                                                                KnowledgeMetadata.SOURCE_CHECKSUM));

                assertEquals(
                                "content-checksum",
                                restored.getMetadata()
                                                .get(
                                                                KnowledgeMetadata.CONTENT_CHECKSUM));
        }

        private CachedRetrievalResult roundTrip(
                        CachedRetrievalResult result) {

                return serializer.deserialize(
                                serializer.serialize(result));
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

        private void assertDocumentEquals(
                        Document expected,
                        Document actual) {

                assertEquals(
                                expected.getId(),
                                actual.getId());

                assertEquals(
                                expected.getText(),
                                actual.getText());

                assertEquals(
                                expected.getScore(),
                                actual.getScore());

                assertEquals(
                                expected.getMetadata(),
                                actual.getMetadata());
        }
}