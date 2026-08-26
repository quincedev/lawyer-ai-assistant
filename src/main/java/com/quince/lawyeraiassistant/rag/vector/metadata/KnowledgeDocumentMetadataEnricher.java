package com.quince.lawyeraiassistant.rag.vector.metadata;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.quince.lawyeraiassistant.rag.config.ConditionalOnRagEnabled;
import com.quince.lawyeraiassistant.rag.vector.config.KnowledgeBaseProperties;
import com.quince.lawyeraiassistant.rag.vector.identity.KnowledgeDocumentIdentityFactory;
import com.quince.lawyeraiassistant.retrieval.tenant.KnowledgeMetadata;
import com.quince.lawyeraiassistant.retrieval.tenant.KnowledgeScope;

import lombok.RequiredArgsConstructor;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnRagEnabled
@RequiredArgsConstructor
public class KnowledgeDocumentMetadataEnricher {

        private final KnowledgeDocumentIdentityFactory identityFactory;

        private final KnowledgeBaseProperties properties;

        public Document enrichParent(
                        Document document,
                        String sourceName,
                        String sourceUri,
                        String sourceChecksum,
                        int pageNumber) {

                Objects.requireNonNull(
                                document,
                                "document must not be null");

                String text = document.getText();

                if (text == null || text.isBlank()) {
                        throw new IllegalArgumentException(
                                        "Parent document text must not be blank");
                }

                if (sourceName == null || sourceName.isBlank()) {
                        throw new IllegalArgumentException(
                                        "sourceName must not be blank");
                }

                if (sourceUri == null || sourceUri.isBlank()) {
                        throw new IllegalArgumentException(
                                        "sourceUri must not be blank");
                }

                if (sourceChecksum == null
                                || sourceChecksum.isBlank()) {

                        throw new IllegalArgumentException(
                                        "sourceChecksum must not be blank");
                }

                if (pageNumber < 1) {
                        throw new IllegalArgumentException(
                                        "pageNumber must be greater than zero");
                }

                String contentChecksum = identityFactory.checksum(text);

                String sourceDocumentId = identityFactory.sourceDocumentId(
                                sourceUri,
                                sourceChecksum);

                String parentDocumentId = identityFactory.parentDocumentId(
                                sourceDocumentId,
                                pageNumber,
                                contentChecksum);

                Map<String, Object> metadata = new LinkedHashMap<>(
                                document.getMetadata());

                metadata.put(
                                KnowledgeMetadata.SOURCE_DOCUMENT_ID,
                                sourceDocumentId);

                metadata.put(
                                KnowledgeMetadata.SOURCE_NAME,
                                sourceName.trim());

                metadata.put(
                                KnowledgeMetadata.SOURCE_URI,
                                sourceUri.trim());

                metadata.put(
                                KnowledgeMetadata.SOURCE_CHECKSUM,
                                sourceChecksum.trim());

                metadata.put(
                                KnowledgeMetadata.PARENT_DOCUMENT_ID,
                                parentDocumentId);

                metadata.put(
                                KnowledgeMetadata.PAGE_NUMBER,
                                pageNumber);

                metadata.put(
                                KnowledgeMetadata.CONTENT_CHECKSUM,
                                contentChecksum);

                metadata.put(
                                KnowledgeMetadata.KNOWLEDGE_VERSION,
                                properties.version());

                metadata.put(
                                KnowledgeMetadata.KNOWLEDGE_SCOPE,
                                KnowledgeScope.SHARED.name());

                /*
                 * 公共知识不携带 tenant_id。
                 */
                metadata.remove(
                                KnowledgeMetadata.TENANT_ID);

                /*
                 * Parent 不携带 Chunk 专属字段。
                 */
                metadata.remove(
                                KnowledgeMetadata.CHUNK_ID);

                metadata.remove(
                                KnowledgeMetadata.CHUNK_INDEX);

                return Document.builder()
                                .id(parentDocumentId)
                                .text(text)
                                .metadata(metadata)
                                .build();
        }

        public Document enrichChunk(
                        Document chunk,
                        Document parent,
                        int chunkIndex) {

                Objects.requireNonNull(
                                chunk,
                                "chunk must not be null");

                Objects.requireNonNull(
                                parent,
                                "parent must not be null");

                if (chunkIndex < 0) {
                        throw new IllegalArgumentException(
                                        "chunkIndex must not be negative");
                }

                String text = chunk.getText();

                if (text == null || text.isBlank()) {
                        throw new IllegalArgumentException(
                                        "Chunk text must not be blank");
                }

                String parentDocumentId = parent.getId();

                if (parentDocumentId == null
                                || parentDocumentId.isBlank()) {

                        throw new IllegalArgumentException(
                                        "Parent document id must not be blank");
                }

                String contentChecksum = identityFactory.checksum(text);

                String chunkId = identityFactory.chunkId(
                                parentDocumentId,
                                chunkIndex,
                                contentChecksum);

                /*
                 * 从 Parent 复制来源、版本、租户范围和
                 * source_checksum 等 metadata。
                 */
                Map<String, Object> metadata = new LinkedHashMap<>(
                                parent.getMetadata());

                metadata.put(
                                KnowledgeMetadata.PARENT_DOCUMENT_ID,
                                parentDocumentId);

                metadata.put(
                                KnowledgeMetadata.CHUNK_ID,
                                chunkId);

                metadata.put(
                                KnowledgeMetadata.CHUNK_INDEX,
                                chunkIndex);

                /*
                 * Chunk 的 content_checksum 对应 Chunk 正文，
                 * 不能继续使用 Parent 正文的 checksum。
                 */
                metadata.put(
                                KnowledgeMetadata.CONTENT_CHECKSUM,
                                contentChecksum);

                return Document.builder()
                                .id(chunkId)
                                .text(text)
                                .metadata(metadata)
                                .build();
        }
}
