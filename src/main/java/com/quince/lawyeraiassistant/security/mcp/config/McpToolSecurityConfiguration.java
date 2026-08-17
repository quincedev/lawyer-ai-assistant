package com.quince.lawyeraiassistant.security.mcp.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(McpToolSecurityProperties.class)
public class McpToolSecurityConfiguration {
}