package com.quince.lawyeraiassistant.agent.tool.legal;

import com.quince.lawyeraiassistant.retrieval.formatter.LegalRetrievalResultFormatter;
import com.quince.lawyeraiassistant.retrieval.orchestration.RetrievalOrchestrator;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class LegalKnowledgeToolConditionalTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    LegalKnowledgeToolTestConfiguration.class);

    @Test
    void shouldCreateLocalLegalToolWhenRagIsEnabled() {

        contextRunner
                .withPropertyValues(
                        "app.agent.legal-tool-mode=local",
                        "app.rag.knowledge-base.enabled=true")
                .run(context -> {

                    assertThat(context)
                            .hasNotFailed();

                    assertThat(context)
                            .hasSingleBean(
                                    LegalKnowledgeTool.class);
                });
    }

    @Test
    void shouldNotCreateLocalLegalToolWhenRagIsDisabled() {

        contextRunner
                .withPropertyValues(
                        "app.agent.legal-tool-mode=local",
                        "app.rag.knowledge-base.enabled=false")
                .run(context -> {

                    assertThat(context)
                            .hasNotFailed();

                    assertThat(context)
                            .doesNotHaveBean(
                                    LegalKnowledgeTool.class);
                });
    }

    @Test
    void shouldNotCreateLocalLegalToolInMcpMode() {

        contextRunner
                .withPropertyValues(
                        "app.agent.legal-tool-mode=mcp",
                        "app.rag.knowledge-base.enabled=true")
                .run(context -> {

                    assertThat(context)
                            .hasNotFailed();

                    assertThat(context)
                            .doesNotHaveBean(
                                    LegalKnowledgeTool.class);
                });
    }

    @Test
    void shouldNotCreateLocalToolWhenBothConditionsAreDisabled() {

        contextRunner
                .withPropertyValues(
                        "app.agent.legal-tool-mode=mcp",
                        "app.rag.knowledge-base.enabled=false")
                .run(context -> {

                    assertThat(context)
                            .hasNotFailed();

                    assertThat(context)
                            .doesNotHaveBean(
                                    LegalKnowledgeTool.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @Import(LegalKnowledgeTool.class)
    static class LegalKnowledgeToolTestConfiguration {

        @Bean
        RetrievalOrchestrator retrievalOrchestrator() {

            return mock(
                    RetrievalOrchestrator.class);
        }

        @Bean
        LegalRetrievalResultFormatter legalRetrievalResultFormatter() {

            return new LegalRetrievalResultFormatter();
        }
    }
}