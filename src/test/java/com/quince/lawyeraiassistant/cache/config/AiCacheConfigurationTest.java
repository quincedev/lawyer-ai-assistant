package com.quince.lawyeraiassistant.cache.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.quince.lawyeraiassistant.cache.redis.CachedRetrievalResultRedisSerializer;
import com.quince.lawyeraiassistant.cache.redis.RedisDocumentMapper;
import com.quince.lawyeraiassistant.cache.redis.RedisMetadataValidator;
import com.quince.lawyeraiassistant.cache.redis.RedisRetrievalCache;
import com.quince.lawyeraiassistant.cache.redis.RedisToolResultCache;
import com.quince.lawyeraiassistant.cache.redis.StringRedisCacheSerializer;
import com.quince.lawyeraiassistant.cache.retrieval.CaffeineRetrievalCache;
import com.quince.lawyeraiassistant.cache.retrieval.RetrievalCache;
import com.quince.lawyeraiassistant.cache.tool.CaffeineToolResultCache;
import com.quince.lawyeraiassistant.cache.tool.ToolResultCache;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.StringRedisTemplate;

import tools.jackson.databind.ObjectMapper;

class AiCacheConfigurationTest {

        private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                        .withUserConfiguration(
                                        AiCacheConfiguration.class);

        @Test
        void shouldUseCaffeineByDefaultWithoutRedisDependencies() {

                contextRunner.run(context -> {

                        assertThat(context)
                                        .hasSingleBean(
                                                        RetrievalCache.class);

                        assertThat(context)
                                        .hasSingleBean(
                                                        ToolResultCache.class);

                        assertThat(context.getBean(
                                        RetrievalCache.class))
                                        .isInstanceOf(
                                                        CaffeineRetrievalCache.class);

                        assertThat(context.getBean(
                                        ToolResultCache.class))
                                        .isInstanceOf(
                                                        CaffeineToolResultCache.class);

                        /*
                         * 重点验证默认模式完全不创建 Redis 组件，
                         * 即使上下文中没有 ObjectMapper 和
                         * StringRedisTemplate 也能启动。
                         */
                        assertThat(context)
                                        .doesNotHaveBean(
                                                        RedisRetrievalCache.class);

                        assertThat(context)
                                        .doesNotHaveBean(
                                                        RedisToolResultCache.class);

                        assertThat(context)
                                        .doesNotHaveBean(
                                                        CachedRetrievalResultRedisSerializer.class);

                        assertThat(context)
                                        .doesNotHaveBean(
                                                        StringRedisCacheSerializer.class);

                        assertThat(context)
                                        .doesNotHaveBean(
                                                        RedisDocumentMapper.class);

                        assertThat(context)
                                        .doesNotHaveBean(
                                                        RedisMetadataValidator.class);
                });
        }

        @Test
        void shouldUseCaffeineWhenExplicitlyConfigured() {

                contextRunner
                                .withPropertyValues(
                                                "app.cache.backend=CAFFEINE")
                                .run(context -> {

                                        assertThat(context.getBean(
                                                        RetrievalCache.class))
                                                        .isInstanceOf(
                                                                        CaffeineRetrievalCache.class);

                                        assertThat(context.getBean(
                                                        ToolResultCache.class))
                                                        .isInstanceOf(
                                                                        CaffeineToolResultCache.class);
                                });
        }

        @Test
        void shouldUseRedisWhenConfigured() {

                contextRunner
                                .withBean(
                                                StringRedisTemplate.class,
                                                () -> mock(
                                                                StringRedisTemplate.class))
                                .withBean(
                                                ObjectMapper.class,
                                                ObjectMapper::new)
                                .withPropertyValues(
                                                "app.cache.backend=REDIS",
                                                "app.cache.redis.key-prefix=test-cache")
                                .run(context -> {

                                        assertThat(context)
                                                        .hasSingleBean(
                                                                        RetrievalCache.class);

                                        assertThat(context)
                                                        .hasSingleBean(
                                                                        ToolResultCache.class);

                                        assertThat(context.getBean(
                                                        RetrievalCache.class))
                                                        .isInstanceOf(
                                                                        RedisRetrievalCache.class);

                                        assertThat(context.getBean(
                                                        ToolResultCache.class))
                                                        .isInstanceOf(
                                                                        RedisToolResultCache.class);

                                        assertThat(context)
                                                        .hasSingleBean(
                                                                        RedisDocumentMapper.class);

                                        assertThat(context)
                                                        .hasSingleBean(
                                                                        RedisMetadataValidator.class);

                                        assertThat(context)
                                                        .hasSingleBean(
                                                                        CachedRetrievalResultRedisSerializer.class);

                                        assertThat(context)
                                                        .hasSingleBean(
                                                                        StringRedisCacheSerializer.class);

                                        assertThat(context)
                                                        .doesNotHaveBean(
                                                                        CaffeineRetrievalCache.class);

                                        assertThat(context)
                                                        .doesNotHaveBean(
                                                                        CaffeineToolResultCache.class);
                                });
        }
}