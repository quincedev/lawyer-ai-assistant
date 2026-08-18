package com.quince.lawyeraiassistant.security.tenant.quota;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import com.quince.lawyeraiassistant.security.SecurityTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SecurityTest
class TenantResourceQuotaPropertiesIntegrationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    TestConfiguration.class);

    @Test
    void shouldBindValidTenantQuotaConfiguration() {

        contextRunner
                .withPropertyValues(
                        "app.tenant.quota.max-concurrent-agent-executions=5")
                .run(
                        context -> {

                            assertNotNull(
                                    context);

                            TenantResourceQuotaProperties properties = context.getBean(
                                    TenantResourceQuotaProperties.class);

                            assertEquals(
                                    5,
                                    properties
                                            .getMaxConcurrentAgentExecutions());
                        });
    }

    @Test
    void shouldRejectInvalidTenantQuotaConfiguration() {

        contextRunner
                .withPropertyValues(
                        "app.tenant.quota.max-concurrent-agent-executions=0")
                .run(
                        context -> {

                            assertNotNull(
                                    context.getStartupFailure());
                        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(TenantResourceQuotaProperties.class)
    static class TestConfiguration {
    }
}