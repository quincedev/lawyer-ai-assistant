package com.quince.lawyeraiassistant.rag.vector.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Knowledge Base Initialization Configuration。
 *
 * @param enabled  是否执行知识库初始化
 * @param location 知识文件资源位置
 * @param version  知识内容发布版本
 */
@ConfigurationProperties(prefix = "app.rag.knowledge-base")
public record KnowledgeBaseProperties(
                boolean enabled,
                String location,
                String version) {

        private static final String DEFAULT_LOCATION = "classpath:knowledge/";

        private static final String DEFAULT_VERSION = "v1";

        public KnowledgeBaseProperties {

                location = normalizeOrDefault(
                                location,
                                DEFAULT_LOCATION);

                version = normalizeOrDefault(
                                version,
                                DEFAULT_VERSION);
        }

        private static String normalizeOrDefault(
                        String value,
                        String defaultValue) {

                if (value == null || value.isBlank()) {
                        return defaultValue;
                }

                return value.trim();
        }
}