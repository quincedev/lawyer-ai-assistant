package com.quince.lawyeraiassistant.rag.vector.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.quince.lawyeraiassistant.common.exception.ErrorCode;
import com.quince.lawyeraiassistant.common.exception.KnowledgeBaseException;
import com.quince.lawyeraiassistant.rag.config.ConditionalOnRagEnabled;
import com.quince.lawyeraiassistant.rag.splitter.LegalTextSplitter;
import com.quince.lawyeraiassistant.rag.vector.config.KnowledgeBaseProperties;
import com.quince.lawyeraiassistant.rag.vector.identity.KnowledgeDocumentIdentityFactory;
import com.quince.lawyeraiassistant.rag.vector.metadata.KnowledgeDocumentMetadataEnricher;
import com.quince.lawyeraiassistant.rag.vector.sync.LoadedKnowledgeBatch;
import com.quince.lawyeraiassistant.rag.vector.sync.LoadedKnowledgeSource;
import com.quince.lawyeraiassistant.retrieval.tenant.KnowledgeMetadata;

import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnRagEnabled
public class PdfDocumentLoadingService
                implements DocumentLoadingService {

        private final LegalTextSplitter legalTextSplitter;

        private final KnowledgeDocumentMetadataEnricher metadataEnricher;

        private final KnowledgeDocumentIdentityFactory identityFactory;

        private final KnowledgeBaseProperties knowledgeBaseProperties;

        public PdfDocumentLoadingService(
                        LegalTextSplitter legalTextSplitter,
                        KnowledgeDocumentMetadataEnricher metadataEnricher,
                        KnowledgeDocumentIdentityFactory identityFactory,
                        KnowledgeBaseProperties knowledgeBaseProperties) {

                this.legalTextSplitter = Objects.requireNonNull(
                                legalTextSplitter,
                                "legalTextSplitter must not be null");

                this.metadataEnricher = Objects.requireNonNull(
                                metadataEnricher,
                                "metadataEnricher must not be null");

                this.identityFactory = Objects.requireNonNull(
                                identityFactory,
                                "identityFactory must not be null");

                this.knowledgeBaseProperties = Objects.requireNonNull(
                                knowledgeBaseProperties,
                                "knowledgeBaseProperties must not be null");
        }

        @Override
        public LoadedKnowledgeBatch load(
                        String location) {

                Resource[] resources = resolvePdfResources(location);

                List<Resource> sortedResources = Arrays.stream(resources)
                                .filter(Objects::nonNull)
                                .sorted(
                                                Comparator.comparing(
                                                                this::resourceSortKey))
                                .toList();

                Set<String> sourceChecksums = new LinkedHashSet<>();

                List<LoadedKnowledgeSource> sources = new ArrayList<>();

                int duplicateResources = 0;
                int duplicateParents = 0;
                int duplicateChunks = 0;

                for (Resource resource : sortedResources) {

                        LoadedKnowledgeResource loaded = loadResource(
                                        resource,
                                        location);

                        /*
                         * 相同 PDF 字节只处理一次。
                         *
                         * Resource 已按文件名排序，因此相同内容出现多次时，
                         * 稳定地保留排序后的第一份。
                         */
                        if (!sourceChecksums.add(
                                        loaded.sourceChecksum())) {

                                duplicateResources++;

                                log.warn(
                                                "Duplicate knowledge resource skipped. "
                                                                + "sourceName={}, sourceUri={}, sourceChecksum={}",
                                                loaded.sourceName(),
                                                loaded.sourceUri(),
                                                loaded.sourceChecksum());

                                continue;
                        }

                        Map<String, Document> parentsById = new LinkedHashMap<>();
                        Map<String, Document> chunksById = new LinkedHashMap<>();

                        for (Document parent : loaded.parents()) {

                                boolean parentAdded = addUniqueDocument(
                                                parentsById,
                                                parent,
                                                "Parent");

                                if (!parentAdded) {
                                        duplicateParents++;
                                        continue;
                                }

                                List<Document> parentChunks = splitAndEnrich(parent);

                                for (Document chunk : parentChunks) {

                                        boolean chunkAdded = addUniqueDocument(
                                                        chunksById,
                                                        chunk,
                                                        "Chunk");

                                        if (!chunkAdded) {
                                                duplicateChunks++;
                                        }
                                }
                        }

                        sources.add(
                                        new LoadedKnowledgeSource(
                                                        identityFactory.sourceDocumentId(
                                                                        loaded.sourceUri(),
                                                                        loaded.sourceChecksum()),
                                                        loaded.sourceName(),
                                                        loaded.sourceUri(),
                                                        loaded.sourceChecksum(),
                                                        knowledgeBaseProperties.version(),
                                                        List.copyOf(parentsById.values()),
                                                        List.copyOf(chunksById.values())));
                }

                if (sources.isEmpty()) {

                        log.warn(
                                        "No valid knowledge documents were loaded. "
                                                        + "location={}, discoveredResources={}",
                                        location,
                                        sortedResources.size());

                        return new LoadedKnowledgeBatch(List.of());
                }

                LoadedKnowledgeBatch batch = new LoadedKnowledgeBatch(sources);

                log.info(
                                "PDF documents loaded. "
                                                + "discoveredResources={}, loadedResources={}, "
                                                + "duplicateResources={}, parents={}, "
                                                + "duplicateParents={}, chunks={}, "
                                                + "duplicateChunks={}, version={}",
                                sortedResources.size(),
                                sourceChecksums.size(),
                                duplicateResources,
                                batch.parentCount(),
                                duplicateParents,
                                batch.chunkCount(),
                                duplicateChunks,
                                knowledgeBaseProperties.version());

                return batch;
        }

        private LoadedKnowledgeResource loadResource(
                        Resource resource,
                        String location) {

                try {
                        byte[] sourceBytes;

                        try (InputStream inputStream = resource.getInputStream()) {

                                sourceBytes = inputStream.readAllBytes();
                        }

                        String sourceChecksum = identityFactory.checksum(
                                        sourceBytes);

                        String sourceName = resolveSourceName(resource);

                        String sourceUri = resolveLogicalSourceUri(
                                        location,
                                        sourceName);

                        List<Document> pages = new PagePdfDocumentReader(resource)
                                        .get();

                        List<Document> parents = new ArrayList<>();

                        for (int index = 0; index < pages.size(); index++) {

                                Document page = pages.get(index);

                                if (!hasContent(page)) {
                                        continue;
                                }

                                int pageNumber = resolvePageNumber(
                                                page,
                                                index);

                                parents.add(
                                                metadataEnricher.enrichParent(
                                                                page,
                                                                sourceName,
                                                                sourceUri,
                                                                sourceChecksum,
                                                                pageNumber));
                        }

                        return new LoadedKnowledgeResource(
                                        sourceName,
                                        sourceUri,
                                        sourceChecksum,
                                        parents);
                } catch (IOException exception) {
                        throw new KnowledgeBaseException(
                                        ErrorCode.KNOWLEDGE_BASE_DOCUMENT_LOAD_ERROR,
                                        "Failed to load PDF resource: "
                                                        + resource.getDescription(),
                                        exception);
                }
        }

        List<Document> splitAndEnrich(
                        Document parent) {

                Objects.requireNonNull(
                                parent,
                                "parent must not be null");

                List<Document> sourceChunks = legalTextSplitter.split(
                                List.of(parent));

                if (sourceChunks == null
                                || sourceChunks.isEmpty()) {

                        return List.of();
                }

                List<Document> result = new ArrayList<>(
                                sourceChunks.size());

                for (Document chunk : sourceChunks) {

                        if (!hasContent(chunk)) {
                                continue;
                        }

                        /*
                         * chunk_index 使用有效 Chunk 的实际顺序。
                         */
                        int chunkIndex = result.size();

                        result.add(
                                        metadataEnricher.enrichChunk(
                                                        chunk,
                                                        parent,
                                                        chunkIndex));
                }

                return List.copyOf(result);
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

                if (location == null || location.isBlank()) {
                        throw new IllegalArgumentException(
                                        "Knowledge-base location must not be blank");
                }

                String normalized = location.trim();

                if (normalized.endsWith("/")) {
                        return normalized + "*.pdf";
                }

                return normalized + "/*.pdf";
        }

        private String resolveSourceName(
                        Resource resource) {

                String filename = resource.getFilename();

                if (filename == null || filename.isBlank()) {
                        throw new IllegalStateException(
                                        "Knowledge resource filename is unavailable: "
                                                        + resource.getDescription());
                }

                return filename.trim();
        }

        /**
         * 使用 location + filename 作为稳定的逻辑 URI。
         */
        private String resolveLogicalSourceUri(
                        String location,
                        String sourceName) {

                String normalizedLocation = location.trim()
                                .replace('\\', '/');

                if (!normalizedLocation.endsWith("/")) {
                        normalizedLocation = normalizedLocation + "/";
                }

                return normalizedLocation
                                + sourceName;
        }

        private String resourceSortKey(
                        Resource resource) {

                String filename = resource.getFilename();

                if (filename == null) {
                        return resource.getDescription();
                }

                return filename;
        }

        private int resolvePageNumber(
                        Document document,
                        int pageIndex) {

                Object metadataPage = document.getMetadata()
                                .get(
                                                KnowledgeMetadata.PAGE_NUMBER);

                if (metadataPage instanceof Number number
                                && number.intValue() > 0) {

                        return number.intValue();
                }

                if (metadataPage != null) {
                        try {
                                int parsed = Integer.parseInt(
                                                metadataPage
                                                                .toString()
                                                                .trim());

                                if (parsed > 0) {
                                        return parsed;
                                }
                        } catch (NumberFormatException ignored) {
                                /*
                                 * 回退到 Reader 顺序。
                                 */
                        }
                }

                /*
                 * pageIndex 从 0 开始，业务页码从 1 开始。
                 */
                return pageIndex + 1;
        }

        private boolean hasContent(
                        Document document) {

                return document != null
                                && document.getText() != null
                                && !document.getText().isBlank();
        }

        private boolean addUniqueDocument(
                        Map<String, Document> documentsById,
                        Document candidate,
                        String documentType) {

                Objects.requireNonNull(
                                candidate,
                                "candidate must not be null");

                String documentId = candidate.getId();

                if (documentId == null
                                || documentId.isBlank()) {

                        throw new IllegalStateException(
                                        documentType
                                                        + " document id must not be blank");
                }

                Document existing = documentsById.get(
                                documentId);

                if (existing == null) {

                        documentsById.put(
                                        documentId,
                                        candidate);

                        return true;
                }

                /*
                 * 相同稳定 ID、相同正文，视为安全重复。
                 */
                if (Objects.equals(
                                existing.getText(),
                                candidate.getText())
                                && Objects.equals(
                                                contentChecksum(existing),
                                                contentChecksum(candidate))) {

                        log.debug(
                                        "Duplicate {} document skipped. id={}",
                                        documentType,
                                        documentId);

                        return false;
                }

                /*
                 * 稳定 ID 相同但正文或 checksum 不同，
                 * 表示身份算法、metadata 或数据发生冲突。
                 */
                throw new KnowledgeBaseException(
                                ErrorCode.KNOWLEDGE_BASE_INITIALIZATION_ERROR,
                                "%s document identity collision. id=%s"
                                                .formatted(
                                                                documentType,
                                                                documentId));
        }

        private String contentChecksum(
                        Document document) {

                Object value = document.getMetadata()
                                .get(
                                                KnowledgeMetadata.CONTENT_CHECKSUM);

                if (value == null
                                || value.toString().isBlank()) {

                        throw new IllegalStateException(
                                        "Document content checksum is missing. id="
                                                        + document.getId());
                }

                return value.toString();
        }
}
