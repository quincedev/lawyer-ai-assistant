package com.quince.lawyeraiassistant.rag.vector.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.quince.lawyeraiassistant.cache.config.AiCacheProperties;

import org.junit.jupiter.api.Test;

class KnowledgeVersionConsistencyValidatorTest {

    @Test
    void shouldAcceptMatchingVersions() {

        AiCacheProperties cacheProperties = new AiCacheProperties();

        cacheProperties.setKnowledgeVersion(
                "v2");

        KnowledgeBaseProperties knowledgeProperties = new KnowledgeBaseProperties(
                true,
                "classpath:knowledge/",
                "v2");

        KnowledgeVersionConsistencyValidator validator = new KnowledgeVersionConsistencyValidator(
                knowledgeProperties,
                cacheProperties);

        assertDoesNotThrow(
                validator::afterPropertiesSet);
    }

    @Test
    void shouldRejectMismatchedVersions() {

        AiCacheProperties cacheProperties = new AiCacheProperties();

        cacheProperties.setKnowledgeVersion(
                "v1");

        KnowledgeBaseProperties knowledgeProperties = new KnowledgeBaseProperties(
                true,
                "classpath:knowledge/",
                "v2");

        KnowledgeVersionConsistencyValidator validator = new KnowledgeVersionConsistencyValidator(
                knowledgeProperties,
                cacheProperties);

        assertThrows(
                IllegalStateException.class,
                validator::afterPropertiesSet);
    }
}