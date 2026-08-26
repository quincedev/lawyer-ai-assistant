package com.quince.lawyeraiassistant.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.quince.lawyeraiassistant.rag.vector.initializer.KnowledgeBaseInitializer;
import com.quince.lawyeraiassistant.rag.vector.service.VectorSearchService;
import com.quince.lawyeraiassistant.retrieval.orchestration.RetrievalOrchestrator;

import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "spring.ai.deepseek.api-key=test-key",
        "security.jwt.secret=12345678901234567890123456789012",
        "app.rag.knowledge-base.enabled=true",
        "spring.flyway.enabled=false",
        "management.health.redis.enabled=false"
})
@ActiveProfiles("mcp-server")
class McpServerRoleIsolationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void shouldCreateRagRuntime() {

        assertTrue(
                hasBean(
                        EmbeddingModel.class));

        assertTrue(
                hasBean(
                        VectorStore.class));

        assertTrue(
                hasBean(
                        KnowledgeBaseInitializer.class));

        assertTrue(
                hasBean(
                        VectorSearchService.class));

        assertTrue(
                hasBean(
                        RetrievalOrchestrator.class));
    }

    @Test
    void shouldNotCreateMcpClientRole() {

        assertFalse(
                applicationContext
                        .getEnvironment()
                        .getProperty(
                                "spring.ai.mcp.client.enabled",
                                Boolean.class,
                                true));
    }

    @Test
    void shouldStartServerContext() {

        assertNotNull(
                applicationContext);
    }

    private boolean hasBean(
            Class<?> type) {

        return !applicationContext
                .getBeansOfType(type)
                .isEmpty();
    }
}