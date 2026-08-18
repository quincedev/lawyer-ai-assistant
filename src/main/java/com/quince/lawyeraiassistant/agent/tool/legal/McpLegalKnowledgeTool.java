package com.quince.lawyeraiassistant.agent.tool.legal;

import com.quince.lawyeraiassistant.agent.model.ToolAction;
import com.quince.lawyeraiassistant.agent.model.ToolExecutionResult;
import com.quince.lawyeraiassistant.agent.tool.AgentTool;
import com.quince.lawyeraiassistant.agent.tool.ToolExecutionContext;
import com.quince.lawyeraiassistant.security.audit.SecurityAuditEvent;
import com.quince.lawyeraiassistant.security.audit.SecurityAuditEventType;
import com.quince.lawyeraiassistant.security.audit.SecurityAuditLogger;
import com.quince.lawyeraiassistant.security.guardrail.exception.McpToolResultSecurityViolationException;
import com.quince.lawyeraiassistant.security.legal.SecuritySource;
import com.quince.lawyeraiassistant.security.mcp.result.McpToolResultSecurityResult;
import com.quince.lawyeraiassistant.security.mcp.result.McpToolResultSecurityService;
import com.quince.lawyeraiassistant.security.mcp.tenant.McpTenantExecutionTokenService;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * 基于 MCP 的法律知识检索 Agent Tool。
 *
 * <p>
 * 将当前 AgentTool SPI
 * 适配到 Spring AI MCP ToolCallback。
 * </p>
 *
 * <pre>
 * ToolAction
 *      ↓
 * McpLegalKnowledgeTool
 *      ↓
 * ToolCallback
 *      ↓
 * MCP Client
 *      ↓
 * Legal MCP Server
 *      ↓
 * searchLegalKnowledge
 * </pre>
 */
@Component
@ConditionalOnProperty(prefix = "app.agent", name = "legal-tool-mode", havingValue = "mcp")
public class McpLegalKnowledgeTool
                implements AgentTool {

        public static final String TOOL_NAME = LegalToolContract.SEARCH_LEGAL_KNOWLEDGE;

        private final ToolCallback toolCallback;

        private final ObjectMapper objectMapper;

        private final McpToolResultSecurityService resultSecurityService;

        private final SecurityAuditLogger securityAuditLogger;

        private final McpTenantExecutionTokenService tenantExecutionTokenService;

        public McpLegalKnowledgeTool(
                        SyncMcpToolCallbackProvider toolCallbackProvider,
                        ObjectMapper objectMapper,
                        McpToolResultSecurityService resultSecurityService,
                        SecurityAuditLogger securityAuditLogger,
                        McpTenantExecutionTokenService tenantExecutionTokenService) {

                Objects.requireNonNull(
                                toolCallbackProvider,
                                "toolCallbackProvider must not be null");

                this.objectMapper = Objects.requireNonNull(
                                objectMapper,
                                "objectMapper must not be null");

                this.resultSecurityService = Objects.requireNonNull(
                                resultSecurityService,
                                "resultSecurityService must not be null");

                this.securityAuditLogger = Objects.requireNonNull(
                                securityAuditLogger,
                                "securityAuditLogger must not be null");

                this.toolCallback = resolveToolCallback(
                                toolCallbackProvider);

                this.tenantExecutionTokenService = Objects.requireNonNull(
                                tenantExecutionTokenService,
                                "tenantExecutionTokenService must not be null");
        }

        @Override
        public String name() {

                return TOOL_NAME;
        }

        @Override
        public ToolExecutionResult execute(
                        ToolAction action) {

                return execute(
                                ToolExecutionContext.sharedOnly(),
                                action);
        }

        @Override
        public ToolExecutionResult execute(
                        ToolExecutionContext executionContext,
                        ToolAction action) {

                Objects.requireNonNull(
                                executionContext,
                                "ToolExecutionContext must not be null");

                Objects.requireNonNull(
                                action,
                                "ToolAction must not be null");

                validateToolName(
                                action);

                try {
                        Map<String, Object> arguments = new java.util.LinkedHashMap<>(
                                        action.getArguments());

                        if (executionContext.hasTenantContext()) {

                                String executionToken = tenantExecutionTokenService.issue(
                                                executionContext
                                                                .requireTenantContext());

                                arguments.put(
                                                LegalToolContract.EXECUTION_TOKEN,
                                                executionToken);
                        }

                        String argumentsJson = serializeArguments(
                                        arguments);

                        String rawResult = toolCallback.call(
                                        argumentsJson);

                        String result = normalizeMcpResult(
                                        rawResult);

                        if (result == null
                                        || result.isBlank()) {

                                return ToolExecutionResult.failure(
                                                "MCP tool returned empty result");
                        }

                        McpToolResultSecurityResult securityResult = resultSecurityService.evaluate(
                                        TOOL_NAME,
                                        result);

                        if (securityResult.isDenied()) {

                                securityAuditLogger.log(
                                                SecurityAuditEvent.warn(
                                                                SecurityAuditEventType.MCP_RESULT_SECURITY_REJECTED,
                                                                "McpLegalKnowledgeTool",
                                                                securityResult.reason(),
                                                                Map.of(
                                                                                "toolName",
                                                                                securityResult.toolName(),
                                                                                "policyName",
                                                                                securityResult.policyName())));

                                throw new McpToolResultSecurityViolationException(
                                                securityResult);
                        }

                        return ToolExecutionResult.success(
                                        result);

                } catch (McpToolResultSecurityViolationException exception) {

                        return ToolExecutionResult.failure(
                                        "MCP tool result was rejected by security policy");

                } catch (RuntimeException exception) {

                        return ToolExecutionResult.failure(
                                        "MCP tool execution failed");
                }
        }

        @Override
        public SecuritySource resultSecuritySource() {

                return SecuritySource.MCP_RESULT;
        }

        private ToolCallback resolveToolCallback(
                        SyncMcpToolCallbackProvider provider) {

                for (ToolCallback callback : provider.getToolCallbacks()) {

                        String toolName = callback.getToolDefinition()
                                        .name();

                        if (TOOL_NAME.equals(
                                        toolName)) {

                                return callback;
                        }
                }

                throw new IllegalStateException(
                                "MCP tool not found: "
                                                + TOOL_NAME);
        }

        private String serializeArguments(
                        Map<String, Object> arguments) {

                try {

                        return objectMapper.writeValueAsString(
                                        arguments == null
                                                        ? Map.of()
                                                        : arguments);

                } catch (JacksonException exception) {

                        throw new IllegalArgumentException(
                                        "Failed to serialize MCP tool arguments",
                                        exception);
                }
        }

        private void validateToolName(
                        ToolAction action) {

                if (!TOOL_NAME.equals(
                                action.getToolName())) {

                        throw new IllegalArgumentException(
                                        "ToolAction is not intended for "
                                                        + TOOL_NAME
                                                        + ": "
                                                        + action.getToolName());
                }
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

                                if (!content.has("text")) {
                                        continue;
                                }

                                String text = content.get("text")
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
                         * MCP Callback 如果直接返回普通文本，
                         * 不做 JSON 转换，原样返回。
                         */
                }

                return normalized;
        }
}
