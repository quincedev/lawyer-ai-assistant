package com.quince.lawyeraiassistant.rag.vector.sync;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.quince.lawyeraiassistant.rag.config.ConditionalOnRagEnabled;
import com.quince.lawyeraiassistant.rag.vector.service.VectorSearchService;
import com.quince.lawyeraiassistant.retrieval.parent.provider.ParentDocumentStore;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnRagEnabled
@RequiredArgsConstructor
public class KnowledgeBaseSynchronizer {

    private final KnowledgeManifestStore manifestStore;
    private final ParentDocumentStore parentDocumentStore;
    private final VectorSearchService vectorSearchService;
    private final KnowledgeCacheInvalidator cacheInvalidator;

    public KnowledgeSyncResult synchronize(
            LoadedKnowledgeBatch batch) {

        Objects.requireNonNull(batch, "batch must not be null");

        Map<String, LoadedKnowledgeSource> incomingByUri = batch.sources()
                .stream()
                .collect(Collectors.toMap(
                        LoadedKnowledgeSource::sourceUri,
                        Function.identity(),
                        this::rejectDuplicateSourceUri));

        int addedSources = 0;
        int unchangedSources = 0;
        int replacedSources = 0;
        int removedSources = 0;
        int addedParents = 0;
        int addedChunks = 0;
        int removedParents = 0;
        int removedChunks = 0;

        for (LoadedKnowledgeSource incoming : incomingByUri.values()) {
            KnowledgeSourceManifest existing = manifestStore
                    .findBySourceUri(incoming.sourceUri())
                    .orElse(null);

            if (existing == null) {
                storeNewSource(incoming);
                manifestStore.save(KnowledgeSourceManifest.from(incoming));
                addedSources++;
                addedParents += incoming.parents().size();
                addedChunks += incoming.chunks().size();
                continue;
            }

            if (existing.hasSameContentAs(incoming)) {
                unchangedSources++;
                log.info(
                        "Knowledge source unchanged. sourceUri={}, checksum={}, version={}",
                        incoming.sourceUri(),
                        incoming.sourceChecksum(),
                        incoming.knowledgeVersion());
                continue;
            }

            storeNewSource(incoming);
            vectorSearchService.deleteDocuments(existing.chunkIds());
            parentDocumentStore.deleteByIds(existing.parentIds());
            manifestStore.save(KnowledgeSourceManifest.from(incoming));
            replacedSources++;
            addedParents += incoming.parents().size();
            addedChunks += incoming.chunks().size();
            removedParents += existing.parentIds().size();
            removedChunks += existing.chunkIds().size();
        }

        Set<String> incomingUris = new HashSet<>(incomingByUri.keySet());

        for (KnowledgeSourceManifest existing : manifestStore.findAll()) {
            if (incomingUris.contains(existing.sourceUri())) {
                continue;
            }

            vectorSearchService.deleteDocuments(existing.chunkIds());
            parentDocumentStore.deleteByIds(existing.parentIds());
            manifestStore.deleteBySourceUri(existing.sourceUri());
            removedSources++;
            removedParents += existing.parentIds().size();
            removedChunks += existing.chunkIds().size();
        }

        KnowledgeSyncResult result = new KnowledgeSyncResult(
                addedSources,
                unchangedSources,
                replacedSources,
                removedSources,
                addedParents,
                addedChunks,
                removedParents,
                removedChunks);

        /*
         * 只有 Parent Store、VectorStore 和 Manifest
         * 全部同步成功后才清理 Cache。
         */
        cacheInvalidator.invalidateAfter(
                result);

        return result;
    }

    private void storeNewSource(
            LoadedKnowledgeSource source) {

        parentDocumentStore.saveAll(source.parents());
        vectorSearchService.addDocuments(source.chunks());
    }

    private LoadedKnowledgeSource rejectDuplicateSourceUri(
            LoadedKnowledgeSource first,
            LoadedKnowledgeSource second) {

        throw new IllegalStateException(
                "Duplicate source URI in loaded batch: " + first.sourceUri());
    }
}
