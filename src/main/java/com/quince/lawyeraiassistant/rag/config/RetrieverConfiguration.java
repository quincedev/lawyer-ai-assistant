package com.quince.lawyeraiassistant.rag.config;

import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.quince.lawyeraiassistant.retrieval.config.RetrievalProperties;

@Configuration
public class RetrieverConfiguration {

    @Bean
    public DocumentRetriever documentRetriever(
            VectorStore vectorStore,
            RetrievalProperties retrievalProperties) {

        return VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .topK(retrievalProperties.topK())
                .similarityThreshold(
                        retrievalProperties.similarityThreshold())
                .build();
    }
}