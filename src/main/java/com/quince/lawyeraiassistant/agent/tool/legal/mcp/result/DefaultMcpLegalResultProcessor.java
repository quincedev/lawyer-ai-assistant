package com.quince.lawyeraiassistant.agent.tool.legal.mcp.result;

import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.quince.lawyeraiassistant.agent.model.ToolExecutionResult;
import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceContext;
import com.quince.lawyeraiassistant.agent.tool.legal.evidence.LegalEvidenceCompactor;
import com.quince.lawyeraiassistant.knowledge.contract.LegalToolContract;
import com.quince.lawyeraiassistant.security.audit.SecurityAuditEvent;
import com.quince.lawyeraiassistant.security.audit.SecurityAuditEventType;
import com.quince.lawyeraiassistant.security.audit.SecurityAuditLogger;
import com.quince.lawyeraiassistant.security.guardrail.exception.McpToolResultSecurityViolationException;
import com.quince.lawyeraiassistant.security.mcp.result.McpToolResultSecurityResult;
import com.quince.lawyeraiassistant.security.mcp.result.McpToolResultSecurityService;

import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(prefix = "app.agent", name = "legal-tool-mode", havingValue = "mcp")
public final class DefaultMcpLegalResultProcessor
        implements McpLegalResultProcessor {

    private static final Logger log = LoggerFactory.getLogger(
            DefaultMcpLegalResultProcessor.class);

    private static final String TOOL_NAME = LegalToolContract.SEARCH_LEGAL_KNOWLEDGE;

    private static final String AUDIT_SOURCE = "McpLegalKnowledgeTool";

    private final ObjectMapper objectMapper;

    private final McpToolResultSecurityService resultSecurityService;

    private final SecurityAuditLogger securityAuditLogger;

    private final LegalEvidenceCompactor legalEvidenceCompactor;

    private final AgentPerformanceContext performanceContext;

    public DefaultMcpLegalResultProcessor(
            ObjectMapper objectMapper,
            McpToolResultSecurityService resultSecurityService,
            SecurityAuditLogger securityAuditLogger,
            LegalEvidenceCompactor legalEvidenceCompactor,
            AgentPerformanceContext performanceContext) {

        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper must not be null");

        this.resultSecurityService = Objects.requireNonNull(
                resultSecurityService,
                "resultSecurityService must not be null");

        this.securityAuditLogger = Objects.requireNonNull(
                securityAuditLogger,
                "securityAuditLogger must not be null");

        this.legalEvidenceCompactor = Objects.requireNonNull(
                legalEvidenceCompactor,
                "legalEvidenceCompactor must not be null");

        this.performanceContext = Objects.requireNonNull(
                performanceContext,
                "performanceContext must not be null");
    }

    @Override
    public ToolExecutionResult process(
            String rawResult) {

        String normalizedResult = normalizeMcpResult(
                rawResult);

        if (normalizedResult == null
                || normalizedResult.isBlank()) {

            return ToolExecutionResult.failure(
                    "MCP tool returned empty result");
        }

        McpToolResultSecurityResult securityResult = resultSecurityService.evaluate(
                TOOL_NAME,
                normalizedResult);

        if (securityResult.isDenied()) {

            securityAuditLogger.log(
                    SecurityAuditEvent.warn(
                            SecurityAuditEventType.MCP_RESULT_SECURITY_REJECTED,
                            AUDIT_SOURCE,
                            securityResult.reason(),
                            Map.of(
                                    "toolName",
                                    securityResult.toolName(),
                                    "policyName",
                                    securityResult.policyName())));

            throw new McpToolResultSecurityViolationException(
                    securityResult);
        }

        String compactedResult = legalEvidenceCompactor.compact(
                normalizedResult);

        if (compactedResult == null
                || compactedResult.isBlank()) {

            return ToolExecutionResult.failure(
                    "MCP tool result became empty after evidence compaction");
        }

        int originalChars = normalizedResult.length();

        int compactedChars = compactedResult.length();

        performanceContext
                .current()
                .ifPresent(
                        metrics -> metrics.recordEvidenceCompaction(
                                originalChars,
                                compactedChars));

        if (compactedChars < originalChars) {

            log.info(
                    "Legal evidence compacted. toolName={}, originalChars={}, compactedChars={}",
                    TOOL_NAME,
                    originalChars,
                    compactedChars);
        }

        return ToolExecutionResult.success(
                compactedResult);
    }

    private String normalizeMcpResult(
            String result) {

        if (result == null
                || result.isBlank()) {

            return result;
        }

        String normalized = result.trim();

        try {

            var root = objectMapper.readTree(
                    normalized);

            if (!root.isArray()
                    || root.isEmpty()) {

                return normalized;
            }

            StringBuilder builder = new StringBuilder();

            for (var content : root) {

                if (!content.has(
                        "text")) {

                    continue;
                }

                String text = content.get(
                        "text")
                        .asString();

                if (text == null
                        || text.isBlank()) {

                    continue;
                }

                if (!builder.isEmpty()) {

                    builder.append(
                            System.lineSeparator());
                }

                builder.append(
                        text.trim());
            }

            if (!builder.isEmpty()) {

                return builder.toString();
            }

        } catch (RuntimeException ignored) {

            /*
             * Plain text MCP responses are valid.
             */
        }

        return normalized;
    }
}
