package com.quince.lawyeraiassistant.agent.tool.legal.mcp;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceContext;
import com.quince.lawyeraiassistant.agent.tool.ToolExecutionContext;
import com.quince.lawyeraiassistant.knowledge.contract.LegalToolContract;
import com.quince.lawyeraiassistant.performance.PerformanceTimer;
import com.quince.lawyeraiassistant.security.mcp.tenant.McpTenantExecutionTokenService;
import com.quince.lawyeraiassistant.security.tenant.TenantContext;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(prefix = "app.agent", name = "legal-tool-mode", havingValue = "mcp")
public final class SpringAiLegalKnowledgeMcpClient
        implements LegalKnowledgeMcpClient {

    private static final Logger log = LoggerFactory.getLogger(
            SpringAiLegalKnowledgeMcpClient.class);

    private final ToolCallback toolCallback;

    private final ObjectMapper objectMapper;

    private final McpTenantExecutionTokenService tenantExecutionTokenService;

    private final AgentPerformanceContext performanceContext;

    public SpringAiLegalKnowledgeMcpClient(
            SyncMcpToolCallbackProvider toolCallbackProvider,
            ObjectMapper objectMapper,
            McpTenantExecutionTokenService tenantExecutionTokenService,
            AgentPerformanceContext performanceContext) {

        Objects.requireNonNull(
                toolCallbackProvider,
                "toolCallbackProvider must not be null");

        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper must not be null");

        this.tenantExecutionTokenService = Objects.requireNonNull(
                tenantExecutionTokenService,
                "tenantExecutionTokenService must not be null");

        this.performanceContext = Objects.requireNonNull(
                performanceContext,
                "AgentPerformanceContext must not be null");

        this.toolCallback = resolveToolCallback(
                toolCallbackProvider);
    }

    @Override
    public String search(
            ToolExecutionContext executionContext,
            Map<String, Object> arguments) {

        Objects.requireNonNull(
                executionContext,
                "ToolExecutionContext must not be null");

        Map<String, Object> runtimeArguments = buildRuntimeArguments(
                executionContext,
                arguments);

        String argumentsJson = serializeArguments(
                runtimeArguments);

        PerformanceTimer timer = PerformanceTimer.start();

        try {

            return toolCallback.call(
                    argumentsJson);

        } finally {

            long durationMs = timer.elapsedMillis();

            performanceContext
                    .current()
                    .ifPresent(
                            metrics -> metrics.recordMcpCall(
                                    durationMs));

            log.info(
                    "MCP tool call finished. toolName={}, durationMs={}",
                    LegalToolContract.SEARCH_LEGAL_KNOWLEDGE,
                    durationMs);
        }
    }

    private Map<String, Object> buildRuntimeArguments(
            ToolExecutionContext executionContext,
            Map<String, Object> originalArguments) {

        Map<String, Object> runtimeArguments = new LinkedHashMap<>(
                originalArguments == null
                        ? Map.of()
                        : originalArguments);

        if (executionContext.hasTenantContext()) {

            TenantContext tenantContext = executionContext.requireTenantContext();

            String executionToken = tenantExecutionTokenService.issue(
                    tenantContext);

            runtimeArguments.put(
                    LegalToolContract.EXECUTION_TOKEN,
                    executionToken);
        }

        return runtimeArguments;
    }

    private ToolCallback resolveToolCallback(
            SyncMcpToolCallbackProvider provider) {

        for (ToolCallback callback : provider.getToolCallbacks()) {

            String toolName = callback.getToolDefinition()
                    .name();

            if (LegalToolContract.SEARCH_LEGAL_KNOWLEDGE.equals(
                    toolName)) {

                return callback;
            }
        }

        throw new IllegalStateException(
                "MCP tool not found: "
                        + LegalToolContract.SEARCH_LEGAL_KNOWLEDGE);
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
}
