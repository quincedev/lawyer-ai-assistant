package com.quince.lawyeraiassistant.config.profile;

import com.quince.lawyeraiassistant.cache.config.AiCacheProperties;
import com.quince.lawyeraiassistant.rag.vector.config.KnowledgeBaseProperties;
import com.quince.lawyeraiassistant.rag.vector.config.KnowledgeVersionConsistencyValidator;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeVersionProfileTest {

        private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                        .withUserConfiguration(
                                        KnowledgeVersionTestConfiguration.class);

        @Test
        void shouldAcceptMatchingKnowledgeVersions() {

                contextRunner
                                .withPropertyValues(
                                                "app.rag.knowledge-base.enabled=true",
                                                "app.rag.knowledge-base.location=classpath:knowledge/",
                                                "app.rag.knowledge-base.version=v1",
                                                "app.cache.knowledge-version=v1")
                                .run(context -> assertThat(context)
                                                .hasNotFailed());
        }

        @Test
        void shouldRejectMismatchedKnowledgeVersions() {

                contextRunner
                                .withPropertyValues(
                                                "app.rag.knowledge-base.enabled=true",
                                                "app.rag.knowledge-base.location=classpath:knowledge/",
                                                "app.rag.knowledge-base.version=v2",
                                                "app.cache.knowledge-version=v1")
                                .run(context -> {

                                        assertThat(context)
                                                        .hasFailed();

                                        assertThat(context.getStartupFailure())
                                                        .hasRootCauseInstanceOf(
                                                                        IllegalStateException.class);

                                        assertThat(context.getStartupFailure())
                                                        .hasStackTraceContaining(
                                                                        "Knowledge version mismatch");
                                });
        }

        @Test
        void shouldFallbackToDefaultVersionWhenKnowledgeVersionIsBlank() {

                contextRunner
                                .withPropertyValues(
                                                "app.rag.knowledge-base.enabled=true",
                                                "app.rag.knowledge-base.location=classpath:knowledge/",
                                                "app.rag.knowledge-base.version=",
                                                "app.cache.knowledge-version=v1")
                                .run(context -> {

                                        assertThat(context)
                                                        .hasNotFailed();

                                        KnowledgeBaseProperties properties = context.getBean(
                                                        KnowledgeBaseProperties.class);

                                        assertThat(properties.version())
                                                        .isEqualTo("v1");

                                        assertThat(context)
                                                        .hasSingleBean(
                                                                        KnowledgeVersionConsistencyValidator.class);
                                });
        }

        @Configuration(proxyBeanMethods = false)
        @EnableConfigurationProperties({
                        AiCacheProperties.class,
                        KnowledgeBaseProperties.class
        })
        @Import(KnowledgeVersionConsistencyValidator.class)
        static class KnowledgeVersionTestConfiguration {
        }
}