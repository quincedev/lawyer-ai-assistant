package com.quince.lawyeraiassistant.mcp.server.legal;

import com.quince.lawyeraiassistant.agent.tool.legal.LegalToolContract;
import com.quince.lawyeraiassistant.retrieval.formatter.LegalRetrievalResultFormatter;
import com.quince.lawyeraiassistant.retrieval.model.RetrieverContext;
import com.quince.lawyeraiassistant.retrieval.orchestration.RetrievalOrchestrator;
import com.quince.lawyeraiassistant.security.audit.SecurityAuditEvent;
import com.quince.lawyeraiassistant.security.audit.SecurityAuditEventType;
import com.quince.lawyeraiassistant.security.audit.SecurityAuditLogger;
import com.quince.lawyeraiassistant.security.guardrail.exception.McpToolSecurityViolationException;
import com.quince.lawyeraiassistant.security.mcp.McpToolSecurityResult;
import com.quince.lawyeraiassistant.security.mcp.McpToolSecurityService;
import com.quince.lawyeraiassistant.security.mcp.tenant.McpTenantExecutionTokenService;
import com.quince.lawyeraiassistant.security.tenant.TenantContext;

import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * Legal MCP Server 暴露的法律能力。
 *
 * <p>
 * 当前 Sprint 4 第一版仅暴露：
 * </p>
 *
 * <pre>
 * searchLegalKnowledge
 * </pre>
 *
 * <p>
 * 本类属于 MCP Server Adapter：
 * </p>
 *
 * <pre>
 * MCP Client
 *      ↓
 * MCP Protocol
 *      ↓
 * LegalMcpTools
 *      ↓
 * RetrievalOrchestrator
 *      ↓
 * LegalRetrievalResultFormatter
 * </pre>
 *
 * <p>
 * 注意：
 * 本类不依赖 LegalKnowledgeTool。
 * Local Agent Tool 和 MCP Tool
 * 共同依赖 Retrieval 能力。
 * </p>
 */
@Slf4j
@Component
@Profile("mcp-server")
public class LegalMcpTools {

        private final RetrievalOrchestrator retrievalOrchestrator;

        private final LegalRetrievalResultFormatter resultFormatter;

        private final McpToolSecurityService mcpToolSecurityService;

        private final SecurityAuditLogger securityAuditLogger;

        private final McpTenantExecutionTokenService tenantExecutionTokenService;

        public LegalMcpTools(
                        RetrievalOrchestrator retrievalOrchestrator,
                        LegalRetrievalResultFormatter resultFormatter,
                        McpToolSecurityService mcpToolSecurityService,
                        SecurityAuditLogger securityAuditLogger,
                        McpTenantExecutionTokenService tenantExecutionTokenService) {

                this.retrievalOrchestrator = Objects.requireNonNull(
                                retrievalOrchestrator,
                                "retrievalOrchestrator must not be null");

                this.resultFormatter = Objects.requireNonNull(
                                resultFormatter,
                                "resultFormatter must not be null");

                this.mcpToolSecurityService = Objects.requireNonNull(
                                mcpToolSecurityService,
                                "mcpToolSecurityService must not be null");

                this.securityAuditLogger = Objects.requireNonNull(
                                securityAuditLogger,
                                "securityAuditLogger must not be null");

                this.tenantExecutionTokenService = Objects.requireNonNull(
                                tenantExecutionTokenService,
                                "tenantExecutionTokenService must not be null");
        }

        /**
         * MCP Tool：
         * 检索中国法律知识库。
         */
        @McpTool(name = LegalToolContract.SEARCH_LEGAL_KNOWLEDGE, description = "检索中国法律知识库，为法律问题分析提供相关法律条文和参考资料")
        public String searchLegalKnowledge(

                        @McpToolParam(description = "需要检索法律依据的法律问题", required = true) String legalQuestion,

                        @McpToolParam(description = "Internal tenant execution token", required = false) String executionToken) {

                String normalizedQuestion = normalizeLegalQuestion(
                                legalQuestion);

                Map<String, Object> arguments = Map.of(
                                "legalQuestion",
                                normalizedQuestion);

                McpToolSecurityResult securityResult = mcpToolSecurityService.evaluate(
                                LegalToolContract.SEARCH_LEGAL_KNOWLEDGE,
                                arguments);

                if (securityResult.isDenied()) {

                        securityAuditLogger.log(
                                        SecurityAuditEvent.warn(
                                                        SecurityAuditEventType.MCP_TOOL_SECURITY_DENIED,
                                                        "LegalMcpTools",
                                                        securityResult.reason(),
                                                        Map.of(
                                                                        "toolName",
                                                                        securityResult.toolName(),
                                                                        "policyName",
                                                                        securityResult.policyName())));

                        throw new McpToolSecurityViolationException(
                                        securityResult);
                }

                log.info(
                                "MCP tool invoked: tool={}",
                                LegalToolContract.SEARCH_LEGAL_KNOWLEDGE);

                TenantContext tenantContext = resolveTenantContext(
                                executionToken);

                RetrieverContext retrievalContext;

                if (tenantContext != null) {

                        retrievalContext = retrievalOrchestrator
                                        .retrieveForTenant(
                                                        normalizedQuestion,
                                                        tenantContext.tenantId());

                } else {

                        /*
                         * No trusted tenant token:
                         * Step 5 guarantees SHARED-only retrieval.
                         */
                        retrievalContext = retrievalOrchestrator.retrieve(
                                        normalizedQuestion);
                }

                Objects.requireNonNull(
                                retrievalContext,
                                "RetrievalOrchestrator must not return null");

                return resultFormatter.format(
                                retrievalContext);
        }

        public String searchLegalKnowledge(
                        String legalQuestion) {

                return searchLegalKnowledge(
                                legalQuestion,
                                null);
        }

        private String normalizeLegalQuestion(
                        String legalQuestion) {

                Objects.requireNonNull(
                                legalQuestion,
                                "Legal question must not be null");

                String normalized = legalQuestion.trim();

                if (normalized.isEmpty()) {

                        throw new IllegalArgumentException(
                                        "Legal question must not be blank");
                }

                return normalized;
        }

        private TenantContext resolveTenantContext(
                        String executionToken) {

                if (executionToken == null
                                || executionToken.isBlank()) {

                        return null;
                }

                try {

                        return tenantExecutionTokenService.verify(
                                        executionToken);

                } catch (RuntimeException exception) {

                        securityAuditLogger.log(
                                        SecurityAuditEvent.warn(
                                                        SecurityAuditEventType.MCP_TENANT_AUTHORIZATION_DENIED,
                                                        "LegalMcpTools",
                                                        "Invalid MCP tenant execution token",
                                                        Map.of(
                                                                        "toolName",
                                                                        LegalToolContract.SEARCH_LEGAL_KNOWLEDGE)));

                        throw exception;
                }
        }
}
