package com.quince.lawyeraiassistant.security.tenant.quota;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Positive;

@Validated
@ConfigurationProperties(prefix = "app.tenant.quota")
public class TenantResourceQuotaProperties {

    @Positive
    private int maxConcurrentAgentExecutions = 3;

    public int getMaxConcurrentAgentExecutions() {

        return maxConcurrentAgentExecutions;
    }

    public void setMaxConcurrentAgentExecutions(
            int maxConcurrentAgentExecutions) {

        this.maxConcurrentAgentExecutions = maxConcurrentAgentExecutions;
    }
}