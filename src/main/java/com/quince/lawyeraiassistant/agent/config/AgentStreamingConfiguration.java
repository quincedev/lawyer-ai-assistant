package com.quince.lawyeraiassistant.agent.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AgentStreamingConfiguration {

    @Bean(name = "agentStreamingExecutor", destroyMethod = "close")
    public ExecutorService agentStreamingExecutor() {

        return Executors
                .newVirtualThreadPerTaskExecutor();
    }
}