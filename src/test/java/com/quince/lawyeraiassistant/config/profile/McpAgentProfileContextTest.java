package com.quince.lawyeraiassistant.config.profile;

import com.quince.lawyeraiassistant.agent.tool.legal.mcp.SpringAiLegalKnowledgeMcpClient;
import com.quince.lawyeraiassistant.controller.RetrievalPlaygroundController;
import com.quince.lawyeraiassistant.rag.vector.initializer.KnowledgeBaseInitializer;
import com.quince.lawyeraiassistant.retrieval.orchestration.RetrievalOrchestrator;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
                "app.cache.backend=CAFFEINE",
                "app.security.identity.store=in-memory",
                "app.rag.knowledge-base.enabled=false",

                /*
                 * 保持 Agent 的真实角色配置，
                 * 但禁止测试期间初始化真实 MCP 网络连接。
                 */
                "spring.ai.mcp.client.initialized=false",
                "spring.ai.mcp.server.enabled=false",

                "spring.ai.deepseek.api-key=test-key",
                "security.jwt.secret=12345678901234567890123456789012",
                "spring.flyway.enabled=false",
                "management.health.redis.enabled=false"
})
@ActiveProfiles("mcp-agent")
class McpAgentProfileContextTest {

        @Autowired
        private ApplicationContext applicationContext;

        @Autowired
        private Environment environment;

        /*
         * 替换真实 MCP Adapter。
         *
         * McpLegalKnowledgeTool 仍会正常创建，
         * 但不会要求真实 SyncMcpToolCallbackProvider，
         * 也不会连接 MCP Server。
         */
        @MockitoBean
        private SpringAiLegalKnowledgeMcpClient legalKnowledgeMcpClient;

        @Test
        void shouldLoadMcpAgentProfile() {

                assertThat(
                                environment.getProperty(
                                                "spring.application.name"))
                                .isEqualTo(
                                                "lawyer-ai-assistant-mcp-agent");

                assertThat(
                                environment.getProperty(
                                                "app.agent.legal-tool-mode"))
                                .isEqualTo("mcp");

                assertThat(
                                environment.getProperty(
                                                "spring.ai.mcp.server.enabled",
                                                Boolean.class))
                                .isFalse();
        }

        @Test
        void shouldNotCreateServerOwnedRagBeans() {

                assertThat(
                                applicationContext.getBeansOfType(
                                                KnowledgeBaseInitializer.class))
                                .isEmpty();

                assertThat(
                                applicationContext.getBeansOfType(
                                                RetrievalOrchestrator.class))
                                .isEmpty();

                assertThat(
                                applicationContext.getBeansOfType(
                                                RetrievalPlaygroundController.class))
                                .isEmpty();
        }

        @Test
        void shouldProvideMcpLegalKnowledgeClientBoundary() {

                assertThat(
                                applicationContext.getBeansOfType(
                                                SpringAiLegalKnowledgeMcpClient.class))
                                .hasSize(1);

                assertThat(legalKnowledgeMcpClient)
                                .isNotNull();
        }
}