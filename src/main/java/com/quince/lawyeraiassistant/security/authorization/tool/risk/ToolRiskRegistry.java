package com.quince.lawyeraiassistant.security.authorization.tool.risk;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;

/**
 * Registry containing security risk metadata
 * for Agent Tool capabilities.
 *
 * <p>
 * Risk metadata is associated with the logical Tool capability
 * rather than its concrete Local / MCP transport implementation.
 * </p>
 */
@Component
public final class ToolRiskRegistry {

    private final Map<String, ToolRiskProfile> profiles;

    public ToolRiskRegistry(
            Collection<ToolRiskProfile> profiles) {

        Objects.requireNonNull(
                profiles,
                "profiles must not be null");

        Map<String, ToolRiskProfile> registered = new LinkedHashMap<>();

        for (ToolRiskProfile profile : profiles) {

            Objects.requireNonNull(
                    profile,
                    "ToolRiskProfile must not be null");

            String toolName = normalize(
                    profile.toolName());

            ToolRiskProfile previous = registered.putIfAbsent(
                    toolName,
                    profile);

            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate ToolRiskProfile: "
                                + toolName);
            }
        }

        this.profiles = Map.copyOf(
                registered);
    }

    public Optional<ToolRiskProfile> find(
            String toolName) {

        if (toolName == null
                || toolName.isBlank()) {

            return Optional.empty();
        }

        return Optional.ofNullable(
                profiles.get(
                        normalize(
                                toolName)));
    }

    public boolean contains(
            String toolName) {

        return find(
                toolName)
                .isPresent();
    }

    public int size() {

        return profiles.size();
    }

    private String normalize(
            String toolName) {

        return toolName
                .strip();
    }
}