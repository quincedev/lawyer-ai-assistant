package com.quince.lawyeraiassistant.rag.config;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.quince.lawyeraiassistant.rag.splitter.LegalTextSplitter;

@Slf4j
@Configuration
@ConditionalOnRagEnabled
@EnableConfigurationProperties({
        RagSplitterProperties.class,
        LegalTextSplitterProperties.class
})
public class RagConfiguration {

    @Bean
    public TokenTextSplitter tokenTextSplitter(
            RagSplitterProperties properties) {

        log.info(
                """
                        初始化 TokenTextSplitter:
                        chunkSize={}
                        minChunkSizeChars={}
                        minChunkLengthToEmbed={}
                        maxNumChunks={}
                        keepSeparator={}
                        """,
                properties.chunkSize(),
                properties.minChunkSizeChars(),
                properties.minChunkLengthToEmbed(),
                properties.maxNumChunks(),
                properties.keepSeparator());

        return TokenTextSplitter.builder()
                .withChunkSize(properties.chunkSize())
                .withMinChunkSizeChars(properties.minChunkSizeChars())
                .withMinChunkLengthToEmbed(
                        properties.minChunkLengthToEmbed())
                .withMaxNumChunks(properties.maxNumChunks())
                .withKeepSeparator(properties.keepSeparator())
                .withPunctuationMarks(
                        List.of('。', '？', '！', '；', '：', '\n'))
                .build();
    }

    @Bean
    public LegalTextSplitter legalTextSplitter(
            LegalTextSplitterProperties properties) {

        return new LegalTextSplitter(
                properties.chunkSize(),
                properties.chunkOverlap(),
                properties.boundarySearchSize(),
                properties.minChunkSize());
    }
}
