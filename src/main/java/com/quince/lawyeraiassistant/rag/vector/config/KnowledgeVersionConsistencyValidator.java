package com.quince.lawyeraiassistant.rag.vector.config;

import java.util.Objects;

import com.quince.lawyeraiassistant.cache.config.AiCacheProperties;
import com.quince.lawyeraiassistant.rag.config.ConditionalOnRagEnabled;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnRagEnabled
public class KnowledgeVersionConsistencyValidator
        implements InitializingBean {

    private final KnowledgeBaseProperties knowledgeBaseProperties;

    private final AiCacheProperties cacheProperties;

    public KnowledgeVersionConsistencyValidator(
            KnowledgeBaseProperties knowledgeBaseProperties,
            AiCacheProperties cacheProperties) {

        this.knowledgeBaseProperties = Objects.requireNonNull(
                knowledgeBaseProperties,
                "knowledgeBaseProperties must not be null");

        this.cacheProperties = Objects.requireNonNull(
                cacheProperties,
                "cacheProperties must not be null");
    }

    @Override
    public void afterPropertiesSet() {

        String knowledgeBaseVersion = normalize(
                knowledgeBaseProperties.version(),
                "app.rag.knowledge-base.version");

        String cacheVersion = normalize(
                cacheProperties.getKnowledgeVersion(),
                "app.cache.knowledge-version");

        if (!knowledgeBaseVersion.equals(
                cacheVersion)) {

            throw new IllegalStateException(
                    "Knowledge version mismatch. "
                            + "app.rag.knowledge-base.version="
                            + knowledgeBaseVersion
                            + ", app.cache.knowledge-version="
                            + cacheVersion);
        }
    }

    private String normalize(
            String value,
            String propertyName) {

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    propertyName
                            + " must not be blank");
        }

        return value.trim();
    }
}
