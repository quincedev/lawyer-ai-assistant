package com.quince.lawyeraiassistant.rag.vector.service;

import com.quince.lawyeraiassistant.rag.splitter.LegalTextSplitter;
import com.quince.lawyeraiassistant.rag.vector.tenant.KnowledgeMetadata;
import com.quince.lawyeraiassistant.rag.vector.tenant.KnowledgeScope;
import com.quince.lawyeraiassistant.retrieval.parent.provider.ParentDocumentStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfDocumentLoadingService
                implements DocumentLoadingService {

        private final LegalTextSplitter legalTextSplitter;

        private final ParentDocumentStore parentDocumentStore;

        @Override
        public List<Document> loadAndSplit(
                        String location) {
                Resource[] resources = resolvePdfResources(location);

                List<Document> sourceDocuments = Arrays.stream(resources)
                                .flatMap(
                                                resource -> new PagePdfDocumentReader(
                                                                resource)
                                                                .get()
                                                                .stream())
                                .filter(
                                                document -> document != null
                                                                && document.getText() != null
                                                                && !document.getText().isBlank())
                                .map(
                                                this::markAsSharedKnowledge)
                                .toList();

                if (sourceDocuments.isEmpty()) {
                        return List.of();
                }

                List<Document> chunks = registerParentsAndSplit(
                                sourceDocuments);

                log.info(
                                "PDF documents loaded. resources={}, parents={}, chunks={}",
                                resources.length,
                                sourceDocuments.size(),
                                chunks.size());

                return chunks;
        }

        List<Document> registerParentsAndSplit(
                        List<Document> sourceDocuments) {
                if (sourceDocuments == null
                                || sourceDocuments.isEmpty()) {
                        return List.of();
                }

                parentDocumentStore.saveAll(
                                sourceDocuments);

                return legalTextSplitter.split(
                                sourceDocuments);
        }

        private Resource[] resolvePdfResources(
                        String location) {
                String pattern = normalizeLocation(location);

                try {
                        return new PathMatchingResourcePatternResolver()
                                        .getResources(pattern);
                } catch (IOException exception) {
                        throw new IllegalStateException(
                                        "Failed to load knowledge-base resources from: "
                                                        + pattern,
                                        exception);
                }
        }

        private String normalizeLocation(
                        String location) {
                if (location.endsWith("/")) {
                        return location + "*.pdf";
                }

                return location + "/*.pdf";
        }

        private Document markAsSharedKnowledge(
                        Document document) {

                Objects.requireNonNull(
                                document,
                                "document must not be null");

                Map<String, Object> metadata = new LinkedHashMap<>(
                                document.getMetadata());

                metadata.put(
                                KnowledgeMetadata.KNOWLEDGE_SCOPE,
                                KnowledgeScope.SHARED.name());

                /*
                 * 公共知识明确不携带 tenant_id。
                 *
                 * 避免某次重新加载时把遗留 tenant metadata
                 * 带进公共知识库。
                 */
                metadata.remove(
                                KnowledgeMetadata.TENANT_ID);

                return Document.builder()
                                .id(
                                                document.getId())
                                .text(
                                                document.getText())
                                .metadata(
                                                metadata)
                                .build();
        }
}