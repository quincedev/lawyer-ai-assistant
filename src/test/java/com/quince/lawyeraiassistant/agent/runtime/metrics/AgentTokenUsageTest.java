package com.quince.lawyeraiassistant.agent.runtime.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AgentTokenUsageTest {

    @Test
    void shouldCreateAvailableUsage() {

        AgentTokenUsage usage = AgentTokenUsage.available(
                1_000,
                200,
                1_200);

        assertEquals(
                1_000,
                usage.promptTokens());

        assertEquals(
                200,
                usage.completionTokens());

        assertEquals(
                1_200,
                usage.totalTokens());

        assertTrue(
                usage.available());
    }

    @Test
    void shouldCreateUnavailableUsage() {

        AgentTokenUsage usage = AgentTokenUsage.unavailable();

        assertEquals(
                0,
                usage.promptTokens());

        assertEquals(
                0,
                usage.completionTokens());

        assertEquals(
                0,
                usage.totalTokens());

        assertFalse(
                usage.available());
    }

    @Test
    void shouldMapCompleteProviderValues() {

        AgentTokenUsage usage = AgentTokenUsage.fromProviderValues(
                100,
                20,
                120);

        assertTrue(
                usage.available());

        assertEquals(
                120,
                usage.totalTokens());
    }

    @Test
    void shouldTreatMissingProviderValueAsUnavailable() {

        AgentTokenUsage usage = AgentTokenUsage.fromProviderValues(
                100,
                null,
                120);

        assertFalse(
                usage.available());
    }

    @Test
    void shouldTreatNegativeProviderValueAsUnavailable() {

        AgentTokenUsage usage = AgentTokenUsage.fromProviderValues(
                -1,
                20,
                19);

        assertFalse(
                usage.available());
    }

    @Test
    void shouldRejectNegativeInternalUsage() {

        assertThrows(
                IllegalArgumentException.class,
                () -> AgentTokenUsage.available(
                        -1,
                        0,
                        0));
    }
}