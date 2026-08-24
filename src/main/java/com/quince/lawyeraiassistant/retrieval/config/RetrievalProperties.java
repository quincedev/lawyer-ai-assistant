package com.quince.lawyeraiassistant.retrieval.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

@Validated
@ConfigurationProperties(prefix = "app.rag.retrieval")
public record RetrievalProperties(

        @Min(1) 
        int topK,

        @DecimalMin("0.0") 
        @DecimalMax("1.0") 
        double similarityThreshold

) {
}