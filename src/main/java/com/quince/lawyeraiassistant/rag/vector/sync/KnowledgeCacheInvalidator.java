package com.quince.lawyeraiassistant.rag.vector.sync;

public interface KnowledgeCacheInvalidator {

    void invalidateAfter(
            KnowledgeSyncResult result);
}