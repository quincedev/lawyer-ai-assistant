package com.quince.lawyeraiassistant.agent.runtime.cost;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class PropertiesModelPricingCatalog
        implements ModelPricingCatalog {

    private static final Logger log = LoggerFactory.getLogger(
            PropertiesModelPricingCatalog.class);

    private final Map<String, ModelPricing> pricingByModel;

    public PropertiesModelPricingCatalog(
            AgentCostProperties properties) {

        String currency = properties.getCurrency()
                .trim()
                .toUpperCase(Locale.ROOT);

        this.pricingByModel = properties.getModels()
                .entrySet()
                .stream()
                .collect(
                        Collectors.toUnmodifiableMap(
                                entry -> normalize(
                                        entry.getKey()),
                                entry -> new ModelPricing(
                                        entry.getKey(),
                                        currency,
                                        entry.getValue()
                                                .getPromptPerMillion(),
                                        entry.getValue()
                                                .getCompletionPerMillion())));

        log.info(
                "LLM pricing catalog initialized. "
                        + "enabled={}, currency={}, models={}",
                properties.isEnabled(),
                currency,
                pricingByModel.keySet());
    }

    @Override
    public Optional<ModelPricing> find(
            String model) {

        if (model == null
                || model.isBlank()) {

            return Optional.empty();
        }

        String normalizedModel = normalize(model);

        Optional<ModelPricing> pricing = Optional.ofNullable(
                pricingByModel.get(
                        normalizedModel));

        if (pricing.isEmpty()) {

            log.debug(
                    "LLM pricing lookup missed. "
                            + "requestedModel={}, configuredModels={}",
                    normalizedModel,
                    pricingByModel.keySet());
        }

        return pricing;
    }

    private static String normalize(
            String model) {

        return model.trim()
                .toLowerCase(Locale.ROOT);
    }
}