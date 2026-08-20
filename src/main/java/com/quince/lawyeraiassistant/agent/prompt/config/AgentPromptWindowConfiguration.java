package com.quince.lawyeraiassistant.agent.prompt.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AgentPromptWindowProperties.class)
public class AgentPromptWindowConfiguration {
}