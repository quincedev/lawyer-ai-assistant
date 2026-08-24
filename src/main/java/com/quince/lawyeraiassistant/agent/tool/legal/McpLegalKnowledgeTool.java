package com.quince.lawyeraiassistant.agent.tool.legal;

import java.util.Objects;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.quince.lawyeraiassistant.agent.model.ToolAction;
import com.quince.lawyeraiassistant.agent.model.ToolExecutionResult;
import com.quince.lawyeraiassistant.agent.tool.AgentTool;
import com.quince.lawyeraiassistant.agent.tool.ToolExecutionContext;
import com.quince.lawyeraiassistant.agent.tool.legal.cache.LegalToolCache;
import com.quince.lawyeraiassistant.agent.tool.legal.mcp.LegalKnowledgeMcpClient;
import com.quince.lawyeraiassistant.agent.tool.legal.mcp.result.McpLegalResultProcessor;
import com.quince.lawyeraiassistant.knowledge.contract.LegalToolContract;
import com.quince.lawyeraiassistant.security.guardrail.exception.McpToolResultSecurityViolationException;
import com.quince.lawyeraiassistant.security.legal.SecuritySource;

@Component
@ConditionalOnProperty(prefix = "app.agent", name = "legal-tool-mode", havingValue = "mcp")
public class McpLegalKnowledgeTool
                implements AgentTool {

        public static final String TOOL_NAME = LegalToolContract.SEARCH_LEGAL_KNOWLEDGE;

        private final LegalKnowledgeMcpClient mcpClient;

        private final McpLegalResultProcessor resultProcessor;

        private final LegalToolCache legalToolCache;

        public McpLegalKnowledgeTool(
                        LegalKnowledgeMcpClient mcpClient,
                        McpLegalResultProcessor resultProcessor,
                        LegalToolCache legalToolCache) {

                this.mcpClient = Objects.requireNonNull(
                                mcpClient,
                                "mcpClient must not be null");

                this.resultProcessor = Objects.requireNonNull(
                                resultProcessor,
                                "resultProcessor must not be null");

                this.legalToolCache = Objects.requireNonNull(
                                legalToolCache,
                                "legalToolCache must not be null");
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

                        /*
                         * Cache stores RAW MCP results.
                         *
                         * A cache HIT must still pass through the result processor
                         * so that security validation and evidence compaction are
                         * never bypassed.
                         */
                        var cacheHit = legalToolCache.get(
                                        executionContext,
                                        action);

                        if (cacheHit.isPresent()) {

                                return resultProcessor.process(
                                                cacheHit.get()
                                                                .rawResult());
                        }

                        /*
                         * Cache MISS:
                         *
                         * ToolAction.arguments remain unchanged.
                         * Runtime MCP arguments such as the tenant execution token
                         * are added by LegalKnowledgeMcpClient.
                         */
                        String rawResult = mcpClient.search(
                                        executionContext,
                                        action.getArguments());

                        if (rawResult == null
                                        || rawResult.isBlank()) {

                                return ToolExecutionResult.failure(
                                                "MCP tool returned empty result");
                        }

                        ToolExecutionResult result = resultProcessor.process(
                                        rawResult);

                        /*
                         * Cache only results that successfully passed the result
                         * processing/security pipeline.
                         *
                         * The RAW MCP result is cached intentionally.
                         */
                        if (result.isSuccess()) {

                                legalToolCache.put(
                                                executionContext,
                                                action,
                                                rawResult);
                        }

                        return result;

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
}