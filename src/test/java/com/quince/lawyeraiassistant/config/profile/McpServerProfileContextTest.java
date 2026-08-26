package com.quince.lawyeraiassistant.config.profile;

import com.quince.lawyeraiassistant.agent.controller.AgentPlaygroundController;
import com.quince.lawyeraiassistant.agent.tool.legal.LegalKnowledgeTool;
import com.quince.lawyeraiassistant.controller.RetrievalPlaygroundController;
import com.quince.lawyeraiassistant.mcp.server.legal.LegalMcpTools;
import com.quince.lawyeraiassistant.rag.vector.initializer.KnowledgeBaseInitializer;
import com.quince.lawyeraiassistant.retrieval.orchestration.RetrievalOrchestrator;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
                "app.cache.backend=CAFFEINE",
                "app.security.identity.store=in-memory",

                /*
                 * Profile Context Test 不加载知识文件，
                 * 不访问 Ollama。
                 */
                "app.rag.knowledge-base.enabled=false",

                "spring.ai.mcp.client.enabled=false",
                "spring.ai.deepseek.api-key=test-key",
                "security.jwt.secret=12345678901234567890123456789012",
                "spring.flyway.enabled=false",
                "management.health.redis.enabled=false"
})
@ActiveProfiles("mcp-server")
class McpServerProfileContextTest {

        @Autowired
        private ApplicationContext applicationContext;

        @Autowired
        private Environment environment;

        @Test
        void shouldLoadMcpServerProfile() {

                assertThat(
                                environment.getProperty(
                                                "spring.application.name"))
                                .isEqualTo(
                                                "lawyer-ai-assistant-mcp-server");

                assertThat(
                                environment.getProperty(
                                                "spring.ai.mcp.server.enabled",
                                                Boolean.class))
                                .isTrue();

                assertThat(
                                environment.getProperty(
                                                "spring.ai.mcp.client.enabled",
                                                Boolean.class))
                                .isFalse();

                assertThat(
                                environment.getProperty(
                                                "app.agent.legal-tool-mode"))
                                .isEqualTo("local");
        }

        @Test
        void shouldNotCreateRagRuntimeWhenKnowledgeBaseIsDisabled() {

                assertThat(
                                applicationContext.getBeansOfType(
                                                RetrievalOrchestrator.class))
                                .isEmpty();

                assertThat(
                                applicationContext.getBeansOfType(
                                                KnowledgeBaseInitializer.class))
                                .isEmpty();

                assertThat(
                                applicationContext.getBeansOfType(
                                                RetrievalPlaygroundController.class))
                                .isEmpty();
        }

        @Test
        void shouldNotCreateLegalToolsWhenRagIsDisabled() {

                assertThat(
                                applicationContext.getBeansOfType(
                                                LegalKnowledgeTool.class))
                                .isEmpty();

                assertThat(
                                applicationContext.getBeansOfType(
                                                LegalMcpTools.class))
                                .isEmpty();
        }

        @Test
        void shouldNotExposeAgentPlaygroundController() {

                assertThat(
                                applicationContext.getBeansOfType(
                                                AgentPlaygroundController.class))
                                .isEmpty();
        }
}