package com.quince.lawyeraiassistant.security.runtime.config;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentRuntimeSecurityPropertiesIntegrationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    TestConfiguration.class);

    @Test
    void shouldBindValidRuntimeSecurityConfiguration() {

        contextRunner
                .withPropertyValues(
                        "agent.runtime.max-steps=12",
                        "agent.runtime.max-tool-calls=9",
                        "agent.runtime.max-replans=3",
                        "agent.runtime.max-retries=4",
                        "agent.runtime.max-execution-time=90s",
                        "agent.runtime.max-tool-execution-time=20s",
                        "agent.runtime.max-observation-length=25000",
                        "agent.runtime.max-context-length=70000")
                .run(
                        context -> {

                            assertTrue(
                                    context.isRunning());

                            AgentRuntimeSecurityProperties properties = context.getBean(
                                    AgentRuntimeSecurityProperties.class);

                            assertEquals(
                                    12,
                                    properties.getMaxSteps());

                            assertEquals(
                                    9,
                                    properties.getMaxToolCalls());

                            assertEquals(
                                    3,
                                    properties.getMaxReplans());

                            assertEquals(
                                    4,
                                    properties.getMaxRetries());

                            assertEquals(
                                    90,
                                    properties
                                            .getMaxExecutionTime()
                                            .toSeconds());

                            assertEquals(
                                    20,
                                    properties
                                            .getMaxToolExecutionTime()
                                            .toSeconds());

                            assertEquals(
                                    25_000,
                                    properties
                                            .getMaxObservationLength());

                            assertEquals(
                                    70_000,
                                    properties
                                            .getMaxContextLength());
                        });
    }

    @Test
    void shouldRejectZeroMaxSteps() {

        contextRunner
                .withPropertyValues(
                        "agent.runtime.max-steps=0")
                .run(
                        context -> assertTrue(
                                context.getStartupFailure() != null));
    }

    @Test
    void shouldRejectNegativeMaxToolCalls() {

        contextRunner
                .withPropertyValues(
                        "agent.runtime.max-tool-calls=-1")
                .run(
                        context -> assertTrue(
                                context.getStartupFailure() != null));
    }

    @Test
    void shouldRejectZeroExecutionTime() {

        contextRunner
                .withPropertyValues(
                        "agent.runtime.max-execution-time=0s")
                .run(
                        context -> assertTrue(
                                context.getStartupFailure() != null));
    }

    @Test
    void shouldRejectNegativeToolExecutionTime() {

        contextRunner
                .withPropertyValues(
                        "agent.runtime.max-tool-execution-time=-1s")
                .run(
                        context -> assertTrue(
                                context.getStartupFailure() != null));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AgentRuntimeSecurityProperties.class)
    static class TestConfiguration {
    }
}