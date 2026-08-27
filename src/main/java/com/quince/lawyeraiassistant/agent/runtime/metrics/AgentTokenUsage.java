package com.quince.lawyeraiassistant.agent.runtime.metrics;

/**
 * Framework-independent token usage reported by one LLM call.
 *
 * <p>
 * This model must not depend on Spring AI metadata types.
 * </p>
 *
 * @param promptTokens     input tokens
 * @param completionTokens output tokens
 * @param totalTokens      total tokens reported by the provider
 * @param available        whether complete usage metadata is available
 */
public record AgentTokenUsage(

        long promptTokens,

        long completionTokens,

        long totalTokens,

        boolean available) {

    public AgentTokenUsage {

        if (promptTokens < 0) {
            throw new IllegalArgumentException(
                    "promptTokens must not be negative");
        }

        if (completionTokens < 0) {
            throw new IllegalArgumentException(
                    "completionTokens must not be negative");
        }

        if (totalTokens < 0) {
            throw new IllegalArgumentException(
                    "totalTokens must not be negative");
        }
    }

    public static AgentTokenUsage available(
            long promptTokens,
            long completionTokens,
            long totalTokens) {

        return new AgentTokenUsage(
                promptTokens,
                completionTokens,
                totalTokens,
                true);
    }

    public static AgentTokenUsage unavailable() {

        return new AgentTokenUsage(
                0,
                0,
                0,
                false);
    }

    /**
     * Converts nullable provider values into the internal contract.
     *
     * <p>
     * Usage is considered unavailable when any required value is
     * missing or negative. Missing values are not silently converted
     * into a confirmed zero-token call.
     * </p>
     */
    public static AgentTokenUsage fromProviderValues(
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens) {

        if (promptTokens == null
                || completionTokens == null
                || totalTokens == null
                || promptTokens < 0
                || completionTokens < 0
                || totalTokens < 0) {

            return unavailable();
        }

        return available(
                promptTokens.longValue(),
                completionTokens.longValue(),
                totalTokens.longValue());
    }
}