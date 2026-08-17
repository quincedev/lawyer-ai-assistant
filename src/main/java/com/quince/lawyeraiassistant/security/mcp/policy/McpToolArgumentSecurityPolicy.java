package com.quince.lawyeraiassistant.security.mcp.policy;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.quince.lawyeraiassistant.agent.tool.legal.LegalToolContract;
import com.quince.lawyeraiassistant.security.mcp.McpToolSecurityPolicy;
import com.quince.lawyeraiassistant.security.mcp.McpToolSecurityResult;
import com.quince.lawyeraiassistant.security.mcp.config.McpToolSecurityProperties;

/**
 * Performs deterministic validation on incoming
 * MCP Tool invocation arguments.
 *
 * <p>
 * Current responsibilities:
 * </p>
 *
 * <ul>
 * <li>required arguments</li>
 * <li>argument type validation</li>
 * <li>blank String rejection</li>
 * <li>String length limit</li>
 * <li>unknown field rejection</li>
 * </ul>
 */
@Component
@Profile("mcp-server")
@Order(20)
public final class McpToolArgumentSecurityPolicy
        implements McpToolSecurityPolicy {

    private static final String NAME = "mcpToolArguments";

    private static final String SEARCH_LEGAL_KNOWLEDGE = LegalToolContract.SEARCH_LEGAL_KNOWLEDGE;

    private static final String LEGAL_QUESTION = "legalQuestion";

    private static final Set<String> SEARCH_LEGAL_KNOWLEDGE_FIELDS = Set.of(
            LEGAL_QUESTION);

    private final int maxStringLength;

    private final boolean rejectUnknownFields;

    public McpToolArgumentSecurityPolicy(
            McpToolSecurityProperties properties) {

        Objects.requireNonNull(
                properties,
                "properties must not be null");

        McpToolSecurityProperties.Arguments arguments = Objects.requireNonNull(
                properties.getArguments(),
                "arguments configuration must not be null");

        this.maxStringLength = arguments.getMaxStringLength();

        this.rejectUnknownFields = arguments.isRejectUnknownFields();
    }

    @Override
    public String name() {

        return NAME;
    }

    @Override
    public McpToolSecurityResult evaluate(
            String toolName,
            Map<String, Object> arguments) {

        Objects.requireNonNull(
                toolName,
                "toolName must not be null");

        Objects.requireNonNull(
                arguments,
                "arguments must not be null");

        String normalizedToolName = toolName.strip();

        if (normalizedToolName.isEmpty()) {
            throw new IllegalArgumentException(
                    "toolName must not be blank");
        }

        /*
         * This policy currently has a concrete schema only for
         * searchLegalKnowledge.
         *
         * Other allowed Tools must receive their own schema rules
         * before they are introduced.
         */
        if (!SEARCH_LEGAL_KNOWLEDGE.equals(
                normalizedToolName)) {

            return McpToolSecurityResult.deny(
                    normalizedToolName,
                    NAME,
                    "No MCP Tool argument schema is configured: "
                            + normalizedToolName);
        }

        McpToolSecurityResult unknownFieldResult = validateUnknownFields(
                normalizedToolName,
                arguments,
                SEARCH_LEGAL_KNOWLEDGE_FIELDS);

        if (unknownFieldResult != null) {
            return unknownFieldResult;
        }

        Object legalQuestion = arguments.get(
                LEGAL_QUESTION);

        if (legalQuestion == null) {

            return McpToolSecurityResult.deny(
                    normalizedToolName,
                    NAME,
                    "Required MCP Tool argument is missing: "
                            + LEGAL_QUESTION);
        }

        if (!(legalQuestion instanceof String question)) {

            return McpToolSecurityResult.deny(
                    normalizedToolName,
                    NAME,
                    "MCP Tool argument must be a String: "
                            + LEGAL_QUESTION);
        }

        if (question.isBlank()) {

            return McpToolSecurityResult.deny(
                    normalizedToolName,
                    NAME,
                    "MCP Tool argument must not be blank: "
                            + LEGAL_QUESTION);
        }

        if (question.length() > maxStringLength) {

            return McpToolSecurityResult.deny(
                    normalizedToolName,
                    NAME,
                    "MCP Tool argument exceeds maximum allowed length: "
                            + LEGAL_QUESTION);
        }

        return McpToolSecurityResult.allow(
                normalizedToolName,
                NAME);
    }

    private McpToolSecurityResult validateUnknownFields(
            String toolName,
            Map<String, Object> arguments,
            Set<String> allowedFields) {

        if (!rejectUnknownFields) {
            return null;
        }

        for (String argumentName : arguments.keySet()) {

            if (argumentName == null
                    || !allowedFields.contains(
                            argumentName)) {

                return McpToolSecurityResult.deny(
                        toolName,
                        NAME,
                        "Unknown MCP Tool argument: "
                                + argumentName);
            }
        }

        return null;
    }
}
