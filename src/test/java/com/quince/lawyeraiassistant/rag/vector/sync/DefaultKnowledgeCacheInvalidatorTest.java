package com.quince.lawyeraiassistant.rag.vector.sync;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.quince.lawyeraiassistant.cache.retrieval.RetrievalCache;
import com.quince.lawyeraiassistant.cache.tool.ToolResultCache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultKnowledgeCacheInvalidatorTest {

    private RetrievalCache retrievalCache;

    private ToolResultCache toolResultCache;

    private DefaultKnowledgeCacheInvalidator invalidator;

    @BeforeEach
    void setUp() {

        retrievalCache = mock(RetrievalCache.class);

        toolResultCache = mock(ToolResultCache.class);

        invalidator = new DefaultKnowledgeCacheInvalidator(
                retrievalCache,
                toolResultCache);
    }

    @Test
    void shouldNotInvalidateWhenKnowledgeIsUnchanged() {

        KnowledgeSyncResult result = new KnowledgeSyncResult(
                0,
                1,
                0,
                0,
                0,
                0,
                0,
                0);

        invalidator.invalidateAfter(
                result);

        verifyNoInteractions(
                retrievalCache,
                toolResultCache);
    }

    @Test
    void shouldInvalidateWhenSourceIsAdded() {

        KnowledgeSyncResult result = new KnowledgeSyncResult(
                1,
                0,
                0,
                0,
                10,
                20,
                0,
                0);

        invalidator.invalidateAfter(
                result);

        verify(retrievalCache)
                .clear();

        verify(toolResultCache)
                .clear();
    }

    @Test
    void shouldInvalidateWhenSourceIsReplaced() {

        KnowledgeSyncResult result = new KnowledgeSyncResult(
                0,
                0,
                1,
                0,
                10,
                20,
                10,
                20);

        invalidator.invalidateAfter(
                result);

        verify(retrievalCache)
                .clear();

        verify(toolResultCache)
                .clear();
    }

    @Test
    void shouldInvalidateWhenSourceIsRemoved() {

        KnowledgeSyncResult result = new KnowledgeSyncResult(
                0,
                0,
                0,
                1,
                0,
                0,
                10,
                20);

        invalidator.invalidateAfter(
                result);

        verify(retrievalCache)
                .clear();

        verify(toolResultCache)
                .clear();
    }
}