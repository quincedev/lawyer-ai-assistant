package com.quince.lawyeraiassistant.cache.config;

import com.quince.lawyeraiassistant.cache.redis.CachedRetrievalResultRedisSerializer;
import com.quince.lawyeraiassistant.cache.redis.RedisCacheKeyMapper;
import com.quince.lawyeraiassistant.cache.redis.RedisCacheSerializer;
import com.quince.lawyeraiassistant.cache.redis.RedisDocumentMapper;
import com.quince.lawyeraiassistant.cache.redis.RedisMetadataValidator;
import com.quince.lawyeraiassistant.cache.redis.RedisNamespaceCleaner;
import com.quince.lawyeraiassistant.cache.redis.RedisRetrievalCache;
import com.quince.lawyeraiassistant.cache.redis.RedisToolResultCache;
import com.quince.lawyeraiassistant.cache.redis.StringRedisCacheSerializer;
import com.quince.lawyeraiassistant.cache.retrieval.CachedRetrievalResult;
import com.quince.lawyeraiassistant.cache.retrieval.CaffeineRetrievalCache;
import com.quince.lawyeraiassistant.cache.retrieval.RetrievalCache;
import com.quince.lawyeraiassistant.cache.tool.CaffeineToolResultCache;
import com.quince.lawyeraiassistant.cache.tool.ToolResultCache;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AiCacheProperties.class)
public class AiCacheConfiguration {

    /*
     * =========================================================
     * Caffeine
     * =========================================================
     */

    @Bean
    @ConditionalOnProperty(prefix = "app.cache", name = "backend", havingValue = "CAFFEINE", matchIfMissing = true)
    public RetrievalCache caffeineRetrievalCache(
            AiCacheProperties properties) {

        AiCacheProperties.CacheSpec spec = properties.getRetrieval();

        return new CaffeineRetrievalCache(
                spec.getMaximumSize(),
                spec.getTtl());
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.cache", name = "backend", havingValue = "CAFFEINE", matchIfMissing = true)
    public ToolResultCache caffeineToolResultCache(
            AiCacheProperties properties) {

        AiCacheProperties.CacheSpec spec = properties.getTool();

        return new CaffeineToolResultCache(
                spec.getMaximumSize(),
                spec.getTtl());
    }

    /*
     * =========================================================
     * Redis Infrastructure
     * =========================================================
     */

    @Bean
    @ConditionalOnProperty(prefix = "app.cache", name = "backend", havingValue = "REDIS")
    public RedisCacheKeyMapper redisCacheKeyMapper(
            AiCacheProperties properties) {

        return new RedisCacheKeyMapper(
                properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.cache", name = "backend", havingValue = "REDIS")
    public RedisNamespaceCleaner redisNamespaceCleaner(
            StringRedisTemplate redisTemplate) {

        return new RedisNamespaceCleaner(
                redisTemplate);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.cache", name = "backend", havingValue = "REDIS")
    public RedisMetadataValidator redisMetadataValidator() {

        return new RedisMetadataValidator();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.cache", name = "backend", havingValue = "REDIS")
    public RedisDocumentMapper redisDocumentMapper(
            RedisMetadataValidator metadataValidator) {

        return new RedisDocumentMapper(
                metadataValidator);
    }

    /*
     * =========================================================
     * Redis Serializers
     * =========================================================
     */

    @Bean("toolResultRedisSerializer")
    @ConditionalOnProperty(prefix = "app.cache", name = "backend", havingValue = "REDIS")
    public RedisCacheSerializer<String> toolResultRedisSerializer() {

        return new StringRedisCacheSerializer();
    }

    @Bean("retrievalRedisSerializer")
    @ConditionalOnProperty(prefix = "app.cache", name = "backend", havingValue = "REDIS")
    public RedisCacheSerializer<CachedRetrievalResult> retrievalRedisSerializer(
            ObjectMapper objectMapper,
            RedisDocumentMapper documentMapper) {

        return new CachedRetrievalResultRedisSerializer(
                objectMapper,
                documentMapper);
    }

    /*
     * =========================================================
     * Redis Adapters
     * =========================================================
     */

    @Bean
    @ConditionalOnProperty(prefix = "app.cache", name = "backend", havingValue = "REDIS")
    public RetrievalCache redisRetrievalCache(
            StringRedisTemplate redisTemplate,
            RedisCacheKeyMapper keyMapper,
            @Qualifier("retrievalRedisSerializer") RedisCacheSerializer<CachedRetrievalResult> serializer,
            RedisNamespaceCleaner namespaceCleaner,
            AiCacheProperties properties) {

        return new RedisRetrievalCache(
                redisTemplate,
                keyMapper,
                serializer,
                namespaceCleaner,
                properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.cache", name = "backend", havingValue = "REDIS")
    public ToolResultCache redisToolResultCache(
            StringRedisTemplate redisTemplate,
            RedisCacheKeyMapper keyMapper,
            @Qualifier("toolResultRedisSerializer") RedisCacheSerializer<String> serializer,
            RedisNamespaceCleaner namespaceCleaner,
            AiCacheProperties properties) {

        return new RedisToolResultCache(
                redisTemplate,
                keyMapper,
                serializer,
                namespaceCleaner,
                properties);
    }
}