package com.quince.lawyeraiassistant.rag.vector.sync;

public record KnowledgeSyncResult(
                int addedSources,
                int unchangedSources,
                int replacedSources,
                int removedSources,
                int addedParents,
                int addedChunks,
                int removedParents,
                int removedChunks) {

        public boolean hasKnowledgeChanges() {

                return addedSources > 0
                                || replacedSources > 0
                                || removedSources > 0;
        }

        public boolean isUnchanged() {

                return !hasKnowledgeChanges();
        }

        public int changedSourceCount() {

                return addedSources
                                + replacedSources
                                + removedSources;
        }
}