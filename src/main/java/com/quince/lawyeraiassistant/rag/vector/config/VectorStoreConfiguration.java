package com.quince.lawyeraiassistant.rag.vector.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.quince.lawyeraiassistant.rag.config.ConditionalOnRagEnabled;
import com.quince.lawyeraiassistant.retrieval.config.RetrievalProperties;

@Configuration
@ConditionalOnRagEnabled
@EnableConfigurationProperties({
        KnowledgeBaseProperties.class,
        RetrievalProperties.class
})
public class VectorStoreConfiguration {

    @Bean
    public VectorStore vectorStore(
            EmbeddingModel embeddingModel) {

        return SimpleVectorStore.builder(embeddingModel)
                .build();
    }
}
