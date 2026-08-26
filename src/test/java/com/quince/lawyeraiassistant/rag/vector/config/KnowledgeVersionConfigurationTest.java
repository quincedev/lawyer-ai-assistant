package com.quince.lawyeraiassistant.rag.vector.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.quince.lawyeraiassistant.cache.config.AiCacheProperties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class KnowledgeVersionConfigurationTest {

    @Test
    void shouldBindSameKnowledgeVersionToRagAndCache() {

        MapConfigurationPropertySource source = new MapConfigurationPropertySource();

        source.put(
                "app.cache.knowledge-version",
                "legal-v2");

        source.put(
                "app.rag.knowledge-base.enabled",
                true);

        source.put(
                "app.rag.knowledge-base.location",
                "classpath:knowledge/");

        source.put(
                "app.rag.knowledge-base.version",
                "legal-v2");

        Binder binder = new Binder(source);

        AiCacheProperties cacheProperties = binder.bind(
                "app.cache",
                AiCacheProperties.class)
                .orElseThrow(() -> new AssertionError(
                        "app.cache properties were not bound"));

        KnowledgeBaseProperties knowledgeProperties = binder.bind(
                "app.rag.knowledge-base",
                KnowledgeBaseProperties.class)
                .orElseThrow(() -> new AssertionError(
                        "app.rag.knowledge-base properties were not bound"));

        assertEquals(
                "legal-v2",
                cacheProperties.getKnowledgeVersion());

        assertEquals(
                "legal-v2",
                knowledgeProperties.version());
    }
}
