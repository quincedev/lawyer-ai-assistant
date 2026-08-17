package com.quince.lawyeraiassistant.security.authorization.tool.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.quince.lawyeraiassistant.security.authorization.tool.risk.ToolRiskProfile;

@Configuration
public class ToolRiskConfiguration {

    @Bean
    ToolRiskProfile legalKnowledgeToolRiskProfile() {

        return ToolRiskProfile.lowReadOnly(
                "searchLegalKnowledge");
    }
}