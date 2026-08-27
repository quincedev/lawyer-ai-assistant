package com.quince.lawyeraiassistant.agent.runtime.cost;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AgentCostProperties.class)
public class AgentCostConfiguration {

    @Bean
    public ModelCostCalculator modelCostCalculator() {

        return new ModelCostCalculator();
    }
}