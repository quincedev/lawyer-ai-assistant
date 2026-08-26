package com.quince.lawyeraiassistant.rag.vector.initializer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import com.quince.lawyeraiassistant.rag.vector.config.KnowledgeBaseProperties;
import com.quince.lawyeraiassistant.rag.vector.service.DocumentLoadingService;
import com.quince.lawyeraiassistant.rag.vector.sync.KnowledgeBaseSynchronizer;
import com.quince.lawyeraiassistant.rag.vector.sync.KnowledgeSyncResult;
import com.quince.lawyeraiassistant.rag.vector.sync.LoadedKnowledgeBatch;
import com.quince.lawyeraiassistant.retrieval.config.RetrievalProperties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;

class KnowledgeBaseInitializerTest {

    @Test
    void shouldLoadBatchAndSynchronizeIt() {

        DocumentLoadingService loader = mock(DocumentLoadingService.class);
        KnowledgeBaseSynchronizer synchronizer = mock(KnowledgeBaseSynchronizer.class);
        LoadedKnowledgeBatch batch = new LoadedKnowledgeBatch(List.of());
        when(loader.load("classpath:knowledge/"))
                .thenReturn(batch);
        when(synchronizer.synchronize(batch))
                .thenReturn(new KnowledgeSyncResult(0, 0, 0, 0, 0, 0, 0, 0));
        KnowledgeBaseInitializer initializer = new KnowledgeBaseInitializer(
                new KnowledgeBaseProperties(true, "classpath:knowledge/", "v1"),
                loader,
                synchronizer,
                new RetrievalProperties(5, 0.7));

        initializer.run(mock(ApplicationArguments.class));

        verify(loader).load("classpath:knowledge/");
        verify(synchronizer).synchronize(batch);
    }

    @Test
    void shouldDoNothingWhenInitializationIsDisabled() {

        DocumentLoadingService loader = mock(DocumentLoadingService.class);
        KnowledgeBaseSynchronizer synchronizer = mock(KnowledgeBaseSynchronizer.class);
        KnowledgeBaseInitializer initializer = new KnowledgeBaseInitializer(
                new KnowledgeBaseProperties(false, "classpath:knowledge/", "v1"),
                loader,
                synchronizer,
                new RetrievalProperties(5, 0.7));

        initializer.run(mock(ApplicationArguments.class));

        verifyNoInteractions(loader, synchronizer);
    }
}
