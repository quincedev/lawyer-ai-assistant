package com.quince.lawyeraiassistant.cache.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.cache")
public class AiCacheProperties {

    private boolean enabled = true;

    /**
     * Knowledge Base 版本。
     *
     * 知识库更新后修改版本，
     * 即可自然使旧 Cache Key 失效。
     */
    @NotNull
    private String knowledgeVersion = "v1";

    @Valid
    private CacheSpec retrieval = new CacheSpec();

    @Valid
    private CacheSpec tool = new CacheSpec();

    public boolean isEnabled() {

        return enabled;
    }

    public void setEnabled(
            boolean enabled) {

        this.enabled = enabled;
    }

    public String getKnowledgeVersion() {

        return knowledgeVersion;
    }

    public void setKnowledgeVersion(
            String knowledgeVersion) {

        this.knowledgeVersion = knowledgeVersion;
    }

    public CacheSpec getRetrieval() {

        return retrieval;
    }

    public void setRetrieval(
            CacheSpec retrieval) {

        this.retrieval = retrieval;
    }

    public CacheSpec getTool() {

        return tool;
    }

    public void setTool(
            CacheSpec tool) {

        this.tool = tool;
    }

    public static class CacheSpec {

        private boolean enabled = true;

        @Min(1)
        private long maximumSize = 1000;

        @NotNull
        private Duration ttl = Duration.ofMinutes(30);

        public boolean isEnabled() {

            return enabled;
        }

        public void setEnabled(
                boolean enabled) {

            this.enabled = enabled;
        }

        public long getMaximumSize() {

            return maximumSize;
        }

        public void setMaximumSize(
                long maximumSize) {

            this.maximumSize = maximumSize;
        }

        public Duration getTtl() {

            return ttl;
        }

        public void setTtl(
                Duration ttl) {

            this.ttl = ttl;
        }
    }
}