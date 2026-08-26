package com.quince.lawyeraiassistant.rag.vector.sync;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.quince.lawyeraiassistant.rag.config.ConditionalOnRagEnabled;

import org.springframework.stereotype.Component;

@Component
@ConditionalOnRagEnabled
public class InMemoryKnowledgeManifestStore
        implements KnowledgeManifestStore {

    private final Map<String, KnowledgeSourceManifest> manifests = new ConcurrentHashMap<>();

    @Override
    public Optional<KnowledgeSourceManifest> findBySourceUri(
            String sourceUri) {

        if (sourceUri == null || sourceUri.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                manifests.get(
                        sourceUri.trim()));
    }

    @Override
    public Collection<KnowledgeSourceManifest> findAll() {

        return List.copyOf(
                manifests.values());
    }

    @Override
    public void save(
            KnowledgeSourceManifest manifest) {

        Objects.requireNonNull(
                manifest,
                "manifest must not be null");

        manifests.put(
                manifest.sourceUri(),
                manifest);
    }

    @Override
    public void deleteBySourceUri(
            String sourceUri) {

        if (sourceUri == null || sourceUri.isBlank()) {
            return;
        }

        manifests.remove(
                sourceUri.trim());
    }
}
