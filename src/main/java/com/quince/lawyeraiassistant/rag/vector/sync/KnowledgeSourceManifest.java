package com.quince.lawyeraiassistant.rag.vector.sync;

import java.util.List;

public record KnowledgeSourceManifest(
        String sourceDocumentId,
        String sourceUri,
        String sourceChecksum,
        String knowledgeVersion,
        List<String> parentIds,
        List<String> chunkIds) {

    public KnowledgeSourceManifest {

        parentIds = parentIds == null
                ? List.of()
                : List.copyOf(parentIds);

        chunkIds = chunkIds == null
                ? List.of()
                : List.copyOf(chunkIds);
    }

    public static KnowledgeSourceManifest from(
            LoadedKnowledgeSource source) {

        return new KnowledgeSourceManifest(
                source.sourceDocumentId(),
                source.sourceUri(),
                source.sourceChecksum(),
                source.knowledgeVersion(),
                source.parentIds(),
                source.chunkIds());
    }

    public boolean hasSameContentAs(
            LoadedKnowledgeSource source) {

        return sourceUri.equals(
                source.sourceUri())
                && sourceChecksum.equals(
                        source.sourceChecksum())
                && knowledgeVersion.equals(
                        source.knowledgeVersion());
    }
}