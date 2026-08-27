package com.quince.lawyeraiassistant.agent.runtime.cost;

import java.util.Optional;

public interface ModelPricingCatalog {

    Optional<ModelPricing> find(
            String model);
}