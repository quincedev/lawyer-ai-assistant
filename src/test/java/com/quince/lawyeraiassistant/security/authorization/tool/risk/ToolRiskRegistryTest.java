package com.quince.lawyeraiassistant.security.authorization.tool.risk;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import com.quince.lawyeraiassistant.security.SecurityTest;

@SecurityTest
class ToolRiskRegistryTest {

    @Test
    void shouldFindRegisteredProfile() {

        ToolRiskProfile profile = ToolRiskProfile.lowReadOnly(
                "searchLegalKnowledge");

        ToolRiskRegistry registry = new ToolRiskRegistry(
                List.of(
                        profile));

        assertTrue(
                registry.find(
                        "searchLegalKnowledge")
                        .isPresent());

        assertEquals(
                profile,
                registry.find(
                        "searchLegalKnowledge")
                        .orElseThrow());

        assertEquals(
                1,
                registry.size());
    }

    @Test
    void shouldNormalizeToolNameDuringLookup() {

        ToolRiskRegistry registry = new ToolRiskRegistry(
                List.of(
                        ToolRiskProfile.lowReadOnly(
                                "searchLegalKnowledge")));

        assertTrue(
                registry.contains(
                        "  searchLegalKnowledge  "));
    }

    @Test
    void shouldReturnEmptyForUnknownTool() {

        ToolRiskRegistry registry = new ToolRiskRegistry(
                List.of(
                        ToolRiskProfile.lowReadOnly(
                                "searchLegalKnowledge")));

        assertTrue(
                registry.find(
                        "unknownTool")
                        .isEmpty());
    }

    @Test
    void shouldReturnEmptyForBlankToolName() {

        ToolRiskRegistry registry = new ToolRiskRegistry(
                List.of());

        assertTrue(
                registry.find(
                        "   ")
                        .isEmpty());

        assertTrue(
                registry.find(
                        null)
                        .isEmpty());
    }

    @Test
    void shouldRejectDuplicateToolProfiles() {

        ToolRiskProfile first = ToolRiskProfile.lowReadOnly(
                "searchLegalKnowledge");

        ToolRiskProfile second = new ToolRiskProfile(
                "searchLegalKnowledge",
                ToolRiskLevel.MEDIUM,
                ToolSideEffectType.WRITE);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> new ToolRiskRegistry(
                        List.of(
                                first,
                                second)));

        assertEquals(
                "Duplicate ToolRiskProfile: searchLegalKnowledge",
                exception.getMessage());
    }

    @Test
    void shouldRejectNullProfiles() {

        assertThrows(
                NullPointerException.class,
                () -> new ToolRiskRegistry(
                        null));
    }
}