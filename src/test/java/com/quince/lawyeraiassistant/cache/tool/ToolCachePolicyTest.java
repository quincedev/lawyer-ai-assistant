package com.quince.lawyeraiassistant.cache.tool;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.quince.lawyeraiassistant.agent.tool.legal.LegalToolContract;

class ToolCachePolicyTest {

    private final ToolCachePolicy policy = new ToolCachePolicy();

    @Test
    void shouldCacheOnlyExactLegalKnowledgeSearchTool() {

        assertTrue(policy.isCacheable(LegalToolContract.SEARCH_LEGAL_KNOWLEDGE));

        assertFalse(policy.isCacheable(null));
        assertFalse(policy.isCacheable(""));
        assertFalse(policy.isCacheable("   "));
        assertFalse(policy.isCacheable(" searchLegalKnowledge"));
        assertFalse(policy.isCacheable("searchLegalKnowledge "));
        assertFalse(policy.isCacheable("SearchLegalKnowledge"));
        assertFalse(policy.isCacheable("searchLegalKnowledgeAdmin"));
        assertFalse(policy.isCacheable("queryCustomer"));
    }
}
