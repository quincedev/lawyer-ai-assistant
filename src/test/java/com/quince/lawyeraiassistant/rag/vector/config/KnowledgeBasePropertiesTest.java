package com.quince.lawyeraiassistant.rag.vector.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KnowledgeBasePropertiesTest {

    @Test
    void shouldPreserveEnabledFlag() {

        KnowledgeBaseProperties enabled = new KnowledgeBaseProperties(
                true,
                "classpath:knowledge/",
                "v1");

        KnowledgeBaseProperties disabled = new KnowledgeBaseProperties(
                false,
                "classpath:knowledge/",
                "v1");

        assertTrue(
                enabled.enabled());

        assertFalse(
                disabled.enabled());
    }

    @Test
    void shouldUseDefaultLocationWhenNull() {

        KnowledgeBaseProperties properties = new KnowledgeBaseProperties(
                true,
                null,
                "v1");

        assertEquals(
                "classpath:knowledge/",
                properties.location());
    }

    @Test
    void shouldUseDefaultLocationWhenBlank() {

        KnowledgeBaseProperties properties = new KnowledgeBaseProperties(
                true,
                "   ",
                "v1");

        assertEquals(
                "classpath:knowledge/",
                properties.location());
    }

    @Test
    void shouldUseDefaultVersionWhenNull() {

        KnowledgeBaseProperties properties = new KnowledgeBaseProperties(
                true,
                "classpath:knowledge/",
                null);

        assertEquals(
                "v1",
                properties.version());
    }

    @Test
    void shouldUseDefaultVersionWhenBlank() {

        KnowledgeBaseProperties properties = new KnowledgeBaseProperties(
                true,
                "classpath:knowledge/",
                "  ");

        assertEquals(
                "v1",
                properties.version());
    }

    @Test
    void shouldTrimConfiguredValues() {

        KnowledgeBaseProperties properties = new KnowledgeBaseProperties(
                true,
                "  classpath:legal/  ",
                "  legal-2026-v2  ");

        assertEquals(
                "classpath:legal/",
                properties.location());

        assertEquals(
                "legal-2026-v2",
                properties.version());
    }
}