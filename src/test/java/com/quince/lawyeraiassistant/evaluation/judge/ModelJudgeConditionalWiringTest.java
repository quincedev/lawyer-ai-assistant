package com.quince.lawyeraiassistant.evaluation.judge;

import com.quince.lawyeraiassistant.agent.runtime.cost.AgentCostProperties;
import com.quince.lawyeraiassistant.agent.runtime.cost.ModelCostCalculator;
import com.quince.lawyeraiassistant.agent.runtime.cost.ModelPricingCatalog;
import com.quince.lawyeraiassistant.evaluation.judge.metrics.MicrometerModelJudgeMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ModelJudgeConditionalWiringTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(JudgeTestConfiguration.class);

    @Test
    void shouldRegisterDisabledJudgeByDefault() {
        contextRunner.run(context -> assertOnlyEvaluator(
                context.getBean(ModelJudgeEvaluator.class),
                DisabledModelJudgeEvaluator.class,
                context.getBeansOfType(ModelJudgeEvaluator.class).size()));
    }

    @Test
    void shouldRegisterDisabledJudgeWhenExplicitlyDisabled() {
        contextRunner.withPropertyValues("app.evaluation.judge.enabled=false")
                .run(context -> assertOnlyEvaluator(
                        context.getBean(ModelJudgeEvaluator.class),
                        DisabledModelJudgeEvaluator.class,
                        context.getBeansOfType(ModelJudgeEvaluator.class).size()));
    }

    @Test
    void shouldRegisterSpringAiJudgeWhenEnabled() {
        contextRunner.withPropertyValues("app.evaluation.judge.enabled=true")
                .run(context -> assertOnlyEvaluator(
                        context.getBean(ModelJudgeEvaluator.class),
                        SpringAiModelJudgeEvaluator.class,
                        context.getBeansOfType(ModelJudgeEvaluator.class).size()));
    }

    @Test
    void shouldNotRegisterRealJudgeInMcpServerProfile() {
        contextRunner.withPropertyValues(
                        "spring.profiles.active=mcp-server",
                        "app.evaluation.judge.enabled=true")
                .run(context -> {
                    assertTrue(context.getBeansOfType(
                            SpringAiModelJudgeEvaluator.class).isEmpty());
                    assertTrue(context.getBeansOfType(
                            ModelJudgeEvaluator.class).isEmpty());
                });
    }

    private void assertOnlyEvaluator(
            ModelJudgeEvaluator evaluator,
            Class<? extends ModelJudgeEvaluator> expectedType,
            int count) {
        assertTrue(expectedType.isInstance(evaluator));
        assertEquals(1, count);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ModelJudgeProperties.class)
    @Import({
            DisabledModelJudgeEvaluator.class,
            SpringAiModelJudgeEvaluator.class,
            ModelJudgePromptFactory.class,
            ModelJudgeResponseParser.class,
            MicrometerModelJudgeMetrics.class
    })
    static class JudgeTestConfiguration {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        ChatModel chatModel() {
            return mock(ChatModel.class);
        }

        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

        @Bean
        ModelPricingCatalog modelPricingCatalog() {
            return model -> Optional.empty();
        }

        @Bean
        ModelCostCalculator modelCostCalculator() {
            return new ModelCostCalculator();
        }

        @Bean
        AgentCostProperties agentCostProperties() {
            return new AgentCostProperties();
        }
    }
}
