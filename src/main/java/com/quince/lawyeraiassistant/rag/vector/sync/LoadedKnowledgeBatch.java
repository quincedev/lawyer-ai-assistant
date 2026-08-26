package com.quince.lawyeraiassistant.rag.vector.sync;

import java.util.List;

public record LoadedKnowledgeBatch(
        List<LoadedKnowledgeSource> sources) {

    public LoadedKnowledgeBatch {

        sources = sources == null
                ? List.of()
                : List.copyOf(sources);
    }

    public int sourceCount() {

        return sources.size();
    }

    public int parentCount() {

        return sources.stream()
                .mapToInt(
                        source -> source.parents()
                                .size())
                .sum();
    }

    public int chunkCount() {

        return sources.stream()
                .mapToInt(
                        source -> source.chunks()
                                .size())
                .sum();
    }
}