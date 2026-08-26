package com.quince.lawyeraiassistant.rag.vector.sync;

import java.util.Collection;
import java.util.Optional;

public interface KnowledgeManifestStore {

    Optional<KnowledgeSourceManifest> findBySourceUri(
            String sourceUri);

    Collection<KnowledgeSourceManifest> findAll();

    void save(
            KnowledgeSourceManifest manifest);

    void deleteBySourceUri(
            String sourceUri);
}