package com.quince.lawyeraiassistant.security.tenant.quota;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TenantResourceQuotaProperties.class)
public class TenantResourceQuotaConfiguration {
}