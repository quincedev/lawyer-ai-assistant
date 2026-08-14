package com.quince.lawyeraiassistant.agent.tool.legal;

import com.quince.lawyeraiassistant.agent.tool.AgentTool;
import com.quince.lawyeraiassistant.agent.tool.AgentToolRegistry;
import com.quince.lawyeraiassistant.retrieval.formatter.LegalRetrievalResultFormatter;
import com.quince.lawyeraiassistant.retrieval.orchestration.RetrievalOrchestrator;

import org.junit.jupiter.api.Test;

import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LegalToolModeRegistrationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    ToolModeTestConfiguration.class);

    @Test
    void shouldRegisterOnlyLocalToolInLocalMode() {

        contextRunner
                .withPropertyValues(
                        "app.agent.legal-tool-mode=local")
                .run(
                        context -> {

                            Map<String, AgentTool> tools = context.getBeansOfType(
                                    AgentTool.class);

                            assertEquals(
                                    1,
                                    tools.size());

                            assertEquals(
                                    1,
                                    context.getBeansOfType(
                                                    LegalKnowledgeTool.class)
                                            .size());

                            assertTrue(
                                    context.getBeansOfType(
                                                    McpLegalKnowledgeTool.class)
                                            .isEmpty());

                            AgentToolRegistry registry = context.getBean(
                                    AgentToolRegistry.class);

                            assertEquals(
                                    1,
                                    registry.size());

                            assertEquals(
                                    LegalKnowledgeTool.TOOL_NAME,
                                    registry.names()
                                            .getFirst());

                            assertTrue(
                                    registry.get(
                                            LegalKnowledgeTool.TOOL_NAME) instanceof LegalKnowledgeTool);
                        });
    }

    @Test
    void shouldRegisterOnlyMcpToolInMcpMode() {

        contextRunner
                .withPropertyValues(
                        "app.agent.legal-tool-mode=mcp")
                .run(
                        context -> {

                            Map<String, AgentTool> tools = context.getBeansOfType(
                                    AgentTool.class);

                            assertEquals(
                                    1,
                                    tools.size());

                            assertTrue(
                                    context.getBeansOfType(
                                                    LegalKnowledgeTool.class)
                                            .isEmpty());

                            assertEquals(
                                    1,
                                    context.getBeansOfType(
                                                    McpLegalKnowledgeTool.class)
                                            .size());

                            AgentToolRegistry registry = context.getBean(
                                    AgentToolRegistry.class);

                            assertEquals(
                                    1,
                                    registry.size());

                            assertEquals(
                                    LegalKnowledgeTool.TOOL_NAME,
                                    registry.names()
                                            .getFirst());

                            assertTrue(
                                    registry.get(
                                            LegalKnowledgeTool.TOOL_NAME) instanceof McpLegalKnowledgeTool);
                        });
    }

    @Test
    void shouldUseLocalModeWhenPropertyIsMissing() {

        contextRunner.run(
                context -> {

                    Map<String, AgentTool> tools = context.getBeansOfType(
                            AgentTool.class);

                    assertEquals(
                            1,
                            tools.size());

                    assertEquals(
                            1,
                            context.getBeansOfType(
                                            LegalKnowledgeTool.class)
                                    .size());

                    assertTrue(
                            context.getBeansOfType(
                                            McpLegalKnowledgeTool.class)
                                    .isEmpty());

                    AgentToolRegistry registry = context.getBean(
                            AgentToolRegistry.class);

                    assertTrue(
                            registry.get(
                                    LegalKnowledgeTool.TOOL_NAME) instanceof LegalKnowledgeTool);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @Import({
            LegalKnowledgeTool.class,
            McpLegalKnowledgeTool.class,
            AgentToolRegistry.class
    })
    static class ToolModeTestConfiguration {

        @Bean
        RetrievalOrchestrator retrievalOrchestrator() {

            return mock(
                    RetrievalOrchestrator.class);
        }

        @Bean
        LegalRetrievalResultFormatter resultFormatter() {

            return new LegalRetrievalResultFormatter();
        }

        @Bean
        ObjectMapper objectMapper() {

            return new ObjectMapper();
        }

        @Bean
        SyncMcpToolCallbackProvider toolCallbackProvider() {

            SyncMcpToolCallbackProvider provider = mock(
                    SyncMcpToolCallbackProvider.class);

            ToolCallback callback = mock(
                    ToolCallback.class);

            ToolDefinition definition = mock(
                    ToolDefinition.class);

            when(
                    definition.name())
                    .thenReturn(
                            LegalKnowledgeTool.TOOL_NAME);

            when(
                    callback.getToolDefinition())
                    .thenReturn(
                            definition);

            when(
                    provider.getToolCallbacks())
                    .thenReturn(
                            new ToolCallback[] {
                                    callback
                            });

            return provider;
        }
    }
}
