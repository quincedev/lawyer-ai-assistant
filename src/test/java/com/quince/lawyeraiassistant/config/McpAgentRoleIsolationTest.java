package com.quince.lawyeraiassistant.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.quince.lawyeraiassistant.agent.tool.legal.mcp.SpringAiLegalKnowledgeMcpClient;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "spring.ai.deepseek.api-key=test-key",
        "spring.ai.mcp.client.initialized=false",
        "security.jwt.secret=12345678901234567890123456789012",
        "spring.flyway.enabled=false",
        "management.health.redis.enabled=false"
})
@ActiveProfiles("mcp-agent")
class McpAgentRoleIsolationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @MockitoBean
    private SpringAiLegalKnowledgeMcpClient legalKnowledgeMcpClient;

    @Test
    void shouldNotCreateLocalRagRuntime() {

        assertFalse(
                applicationContext.containsBeanDefinition(
                        "vectorStore"));

        assertFalse(
                hasBean(
                        EmbeddingModel.class));

        assertFalse(
                hasBean(
                        VectorStore.class));

        assertFalse(
                hasBean(
                        KnowledgeBaseInitializer.class));

        assertFalse(
                hasBean(
                        VectorSearchService.class));

        assertFalse(
                hasBean(
                        RetrievalOrchestrator.class));
    }

    @Test
    void shouldStartAgentContext() {

        assertNotNull(
                applicationContext);
    }

    private boolean hasBean(
            Class<?> type) {

        return applicationContext
                .getBeansOfType(type)
                .isEmpty() == false;
    }
}
