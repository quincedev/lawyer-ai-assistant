package com.quince.lawyeraiassistant.cache.config;

import com.quince.lawyeraiassistant.cache.retrieval.CaffeineRetrievalCache;
import com.quince.lawyeraiassistant.cache.retrieval.RetrievalCache;
import com.quince.lawyeraiassistant.cache.tool.CaffeineToolResultCache;
import com.quince.lawyeraiassistant.cache.tool.ToolResultCache;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AiCacheProperties.class)
public class AiCacheConfiguration {

    @Bean
    public RetrievalCache retrievalCache(
            AiCacheProperties properties) {

        AiCacheProperties.CacheSpec spec = properties.getRetrieval();

        return new CaffeineRetrievalCache(
                spec.getMaximumSize(),
                spec.getTtl());
    }

    @Bean
    public ToolResultCache toolResultCache(
            AiCacheProperties properties) {

        AiCacheProperties.CacheSpec spec = properties.getTool();

        return new CaffeineToolResultCache(
                spec.getMaximumSize(),
                spec.getTtl());
    }
}