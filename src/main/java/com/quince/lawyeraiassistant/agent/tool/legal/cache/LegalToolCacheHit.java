package com.quince.lawyeraiassistant.agent.tool.legal.cache;

import java.util.Objects;

public record LegalToolCacheHit(
        String rawResult) {

    public LegalToolCacheHit {

        Objects.requireNonNull(
                rawResult,
                "rawResult must not be null");
    }
}