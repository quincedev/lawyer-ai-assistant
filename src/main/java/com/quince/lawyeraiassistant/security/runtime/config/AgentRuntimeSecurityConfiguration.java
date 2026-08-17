package com.quince.lawyeraiassistant.security.runtime.config;

import com.quince.lawyeraiassistant.security.runtime.AgentExecutionLimits;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AgentRuntimeSecurityProperties.class)
public class AgentRuntimeSecurityConfiguration {

    @Bean
    public AgentExecutionLimits agentExecutionLimits(
            AgentRuntimeSecurityProperties properties) {

        return new AgentExecutionLimits(
                properties.getMaxSteps(),
                properties.getMaxToolCalls(),
                properties.getMaxReplans(),
                properties.getMaxRetries(),
                properties.getMaxExecutionTime(),
                properties.getMaxToolExecutionTime(),
                properties.getMaxObservationLength(),
                properties.getMaxContextLength());
    }
}