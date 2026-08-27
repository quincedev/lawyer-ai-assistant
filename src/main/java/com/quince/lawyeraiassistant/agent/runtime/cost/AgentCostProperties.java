package com.quince.lawyeraiassistant.agent.runtime.cost;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Validated
@ConfigurationProperties(prefix = "app.observability.cost")
public class AgentCostProperties {

    private boolean enabled = true;

    @NotBlank
    private String currency = "USD";

    @Valid
    private Map<String, PricingSpec> models = new LinkedHashMap<>();

    public boolean isEnabled() {

        return enabled;
    }

    public void setEnabled(
            boolean enabled) {

        this.enabled = enabled;
    }

    public String getCurrency() {

        return currency;
    }

    public void setCurrency(
            String currency) {

        this.currency = currency;
    }

    public Map<String, PricingSpec> getModels() {

        return models;
    }

    public void setModels(
            Map<String, PricingSpec> models) {

        this.models = models;
    }

    public static class PricingSpec {

        @DecimalMin("0.0")
        private BigDecimal promptPerMillion = BigDecimal.ZERO;

        @DecimalMin("0.0")
        private BigDecimal completionPerMillion = BigDecimal.ZERO;

        public BigDecimal getPromptPerMillion() {

            return promptPerMillion;
        }

        public void setPromptPerMillion(
                BigDecimal promptPerMillion) {

            this.promptPerMillion = promptPerMillion;
        }

        public BigDecimal getCompletionPerMillion() {

            return completionPerMillion;
        }

        public void setCompletionPerMillion(
                BigDecimal completionPerMillion) {

            this.completionPerMillion = completionPerMillion;
        }
    }
}