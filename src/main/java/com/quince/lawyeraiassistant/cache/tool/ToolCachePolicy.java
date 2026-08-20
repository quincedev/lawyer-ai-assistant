package com.quince.lawyeraiassistant.cache.tool;

import org.springframework.stereotype.Component;

import com.quince.lawyeraiassistant.agent.tool.legal.LegalToolContract;

@Component
public class ToolCachePolicy {

    public boolean isCacheable(
            String toolName) {

        if (toolName == null
                || toolName.isBlank()) {

            return false;
        }

        return LegalToolContract.SEARCH_LEGAL_KNOWLEDGE
                .equals(
                        toolName);
    }
}