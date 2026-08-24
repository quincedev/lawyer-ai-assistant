package com.quince.lawyeraiassistant.knowledge.contract;

/**
 * Shared contract for legal knowledge tool communication.
 *
 * <p>
 * This contract is intentionally independent from Agent, MCP,
 * Cache and Security implementations.
 * </p>
 */
public final class LegalToolContract {

    public static final String SEARCH_LEGAL_KNOWLEDGE = "searchLegalKnowledge";

    public static final String LEGAL_QUESTION = "legalQuestion";

    public static final String EXECUTION_TOKEN = "_executionToken";

    private LegalToolContract() {
    }
}