package com.quince.lawyeraiassistant.rag.vector.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KnowledgeDocumentIdentityFactoryTest {

        private KnowledgeDocumentIdentityFactory factory;

        @BeforeEach
        void setUp() {

                factory = new KnowledgeDocumentIdentityFactory();
        }

        @Test
        void shouldGenerateStableSourceId() {

                String sourceChecksum = factory.checksum(
                                "pdf-content".getBytes(
                                                StandardCharsets.UTF_8));

                String first = factory.sourceDocumentId(
                                "classpath:knowledge/labor-law.pdf",
                                sourceChecksum);

                String second = factory.sourceDocumentId(
                                "classpath:knowledge/labor-law.pdf",
                                sourceChecksum);

                assertEquals(
                                first,
                                second);

                assertTrue(
                                first.startsWith("source:"));

                assertEquals(
                                71,
                                first.length());
        }

        @Test
        void shouldNormalizeSourceUriWhitespace() {

                String sourceChecksum = factory.checksum(
                                "pdf-content".getBytes(
                                                StandardCharsets.UTF_8));

                assertEquals(
                                factory.sourceDocumentId(
                                                "classpath:knowledge/labor-law.pdf",
                                                sourceChecksum),
                                factory.sourceDocumentId(
                                                "  classpath:knowledge/labor-law.pdf  ",
                                                sourceChecksum));
        }

        @Test
        void shouldChangeSourceIdWhenUriChanges() {

                String sourceChecksum = factory.checksum(
                                "same-pdf".getBytes(
                                                StandardCharsets.UTF_8));

                assertNotEquals(
                                factory.sourceDocumentId(
                                                "classpath:knowledge/a.pdf",
                                                sourceChecksum),
                                factory.sourceDocumentId(
                                                "classpath:knowledge/b.pdf",
                                                sourceChecksum));
        }

        @Test
        void shouldChangeSourceIdWhenSourceContentChanges() {

                String firstChecksum = factory.checksum(
                                "old-pdf".getBytes(
                                                StandardCharsets.UTF_8));

                String secondChecksum = factory.checksum(
                                "new-pdf".getBytes(
                                                StandardCharsets.UTF_8));

                assertNotEquals(
                                factory.sourceDocumentId(
                                                "classpath:knowledge/labor-law.pdf",
                                                firstChecksum),
                                factory.sourceDocumentId(
                                                "classpath:knowledge/labor-law.pdf",
                                                secondChecksum));
        }

        @Test
        void shouldGenerateStableParentId() {

                String sourceId = sourceId();

                String contentChecksum = factory.checksum(
                                "第一页内容");

                assertEquals(
                                factory.parentDocumentId(
                                                sourceId,
                                                1,
                                                contentChecksum),
                                factory.parentDocumentId(
                                                sourceId,
                                                1,
                                                contentChecksum));
        }

        @Test
        void shouldChangeParentIdWhenPageChanges() {

                String sourceId = sourceId();

                String contentChecksum = factory.checksum(
                                "相同内容");

                assertNotEquals(
                                factory.parentDocumentId(
                                                sourceId,
                                                1,
                                                contentChecksum),
                                factory.parentDocumentId(
                                                sourceId,
                                                2,
                                                contentChecksum));
        }

        @Test
        void shouldChangeParentIdWhenContentChanges() {

                String sourceId = sourceId();

                assertNotEquals(
                                factory.parentDocumentId(
                                                sourceId,
                                                1,
                                                factory.checksum("旧内容")),
                                factory.parentDocumentId(
                                                sourceId,
                                                1,
                                                factory.checksum("新内容")));
        }

        @Test
        void shouldNormalizeLineEndingsWhenCalculatingChecksum() {

                assertEquals(
                                factory.checksum(
                                                "第一行\r\n第二行"),
                                factory.checksum(
                                                "第一行\n第二行"));
        }

        @Test
        void shouldGenerateStableChunkId() {

                String parentId = "parent:test";

                String contentChecksum = factory.checksum(
                                "Chunk 内容");

                assertEquals(
                                factory.chunkId(
                                                parentId,
                                                0,
                                                contentChecksum),
                                factory.chunkId(
                                                parentId,
                                                0,
                                                contentChecksum));
        }

        @Test
        void shouldChangeChunkIdWhenIndexChanges() {

                String contentChecksum = factory.checksum(
                                "相同内容");

                assertNotEquals(
                                factory.chunkId(
                                                "parent:test",
                                                0,
                                                contentChecksum),
                                factory.chunkId(
                                                "parent:test",
                                                1,
                                                contentChecksum));
        }

        @Test
        void shouldRejectInvalidArguments() {

                assertThrows(
                                IllegalArgumentException.class,
                                () -> factory.sourceDocumentId(
                                                " ",
                                                "checksum"));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> factory.sourceDocumentId(
                                                "classpath:knowledge/test.pdf",
                                                " "));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> factory.parentDocumentId(
                                                "source:test",
                                                0,
                                                "checksum"));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> factory.chunkId(
                                                "parent:test",
                                                -1,
                                                "checksum"));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> factory.checksum(
                                                " "));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> factory.checksum(
                                                new byte[0]));
        }

        private String sourceId() {

                String sourceChecksum = factory.checksum(
                                "pdf-content".getBytes(
                                                StandardCharsets.UTF_8));

                return factory.sourceDocumentId(
                                "classpath:knowledge/labor-law.pdf",
                                sourceChecksum);
        }
}