package com.quince.lawyeraiassistant.rag.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

@Configuration
@ConditionalOnRagEnabled
public class DocumentConfiguration {

    @Bean
    public Resource laborLawPdf() {

        return new ClassPathResource(
                "documents/劳动合同法.pdf");

    }

}
