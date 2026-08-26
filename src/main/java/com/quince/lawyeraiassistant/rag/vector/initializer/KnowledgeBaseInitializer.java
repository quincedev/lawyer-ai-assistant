package com.quince.lawyeraiassistant.rag.vector.initializer;

import com.quince.lawyeraiassistant.common.exception.ErrorCode;
import com.quince.lawyeraiassistant.common.exception.KnowledgeBaseException;
import com.quince.lawyeraiassistant.rag.config.ConditionalOnRagEnabled;
import com.quince.lawyeraiassistant.rag.vector.config.KnowledgeBaseProperties;
import com.quince.lawyeraiassistant.rag.vector.service.DocumentLoadingService;
import com.quince.lawyeraiassistant.rag.vector.sync.KnowledgeBaseSynchronizer;
import com.quince.lawyeraiassistant.rag.vector.sync.KnowledgeSyncResult;
import com.quince.lawyeraiassistant.rag.vector.sync.LoadedKnowledgeBatch;
import com.quince.lawyeraiassistant.retrieval.config.RetrievalProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnRagEnabled
@RequiredArgsConstructor
public class KnowledgeBaseInitializer implements ApplicationRunner {

    private final KnowledgeBaseProperties properties;
    private final DocumentLoadingService documentLoadingService;
    private final KnowledgeBaseSynchronizer knowledgeBaseSynchronizer;
    private final RetrievalProperties retrievalProperties;

    @Override
    public void run(
            ApplicationArguments args) {

        if (!properties.enabled()) {
            log.info(
                    "Knowledge-base initialization is disabled. "
                            + "version={}",
                    properties.version());

            return;
        }

        log.info(
                "Retrieval configuration: "
                        + "topK={}, similarityThreshold={}",
                retrievalProperties.topK(),
                retrievalProperties.similarityThreshold());

        log.info(
                "Starting knowledge-base initialization. "
                        + "location={}, version={}",
                properties.location(),
                properties.version());

        long startTime = System.currentTimeMillis();

        try {
            LoadedKnowledgeBatch batch = documentLoadingService.load(
                    properties.location());

            KnowledgeSyncResult result = knowledgeBaseSynchronizer.synchronize(
                    batch);

            long duration = System.currentTimeMillis()
                    - startTime;

            log.info(
                    "Knowledge-base synchronization completed. "
                            + "addedSources={}, unchangedSources={}, "
                            + "replacedSources={}, removedSources={}, "
                            + "addedParents={}, addedChunks={}, "
                            + "removedParents={}, removedChunks={}, "
                            + "durationMs={}",
                    result.addedSources(),
                    result.unchangedSources(),
                    result.replacedSources(),
                    result.removedSources(),
                    result.addedParents(),
                    result.addedChunks(),
                    result.removedParents(),
                    result.removedChunks(),
                    duration);
        } catch (KnowledgeBaseException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error(
                    "Knowledge-base initialization failed. "
                            + "location={}, version={}",
                    properties.location(),
                    properties.version(),
                    exception);

            throw new KnowledgeBaseException(
                    ErrorCode.KNOWLEDGE_BASE_INITIALIZATION_ERROR,
                    "知识库初始化失败",
                    exception);
        }
    }
}
