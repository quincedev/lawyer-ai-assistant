package com.quince.lawyeraiassistant.agent.tool.legal;

import com.quince.lawyeraiassistant.agent.model.ToolAction;
import com.quince.lawyeraiassistant.agent.model.ToolExecutionResult;
import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceContext;
import com.quince.lawyeraiassistant.agent.tool.AgentTool;
import com.quince.lawyeraiassistant.agent.tool.ToolExecutionContext;
import com.quince.lawyeraiassistant.agent.tool.legal.evidence.LegalEvidenceCompactor;
import com.quince.lawyeraiassistant.cache.CacheKeyFactory;
import com.quince.lawyeraiassistant.cache.CacheScope;
import com.quince.lawyeraiassistant.cache.config.AiCacheProperties;
import com.quince.lawyeraiassistant.cache.tool.ToolCachePolicy;
import com.quince.lawyeraiassistant.cache.tool.ToolResultCache;
import com.quince.lawyeraiassistant.performance.PerformanceTimer;
import com.quince.lawyeraiassistant.security.audit.SecurityAuditEvent;
import com.quince.lawyeraiassistant.security.audit.SecurityAuditEventType;
import com.quince.lawyeraiassistant.security.audit.SecurityAuditLogger;
import com.quince.lawyeraiassistant.security.guardrail.exception.McpToolResultSecurityViolationException;
import com.quince.lawyeraiassistant.security.legal.SecuritySource;
import com.quince.lawyeraiassistant.security.mcp.result.McpToolResultSecurityResult;
import com.quince.lawyeraiassistant.security.mcp.result.McpToolResultSecurityService;
import com.quince.lawyeraiassistant.security.mcp.tenant.McpTenantExecutionTokenService;
import com.quince.lawyeraiassistant.security.tenant.TenantContext;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 基于 MCP 的法律知识检索 Agent Tool。
 *
 * <p>
 * Step 5 在 Day17/Day18 的安全链路上增加 Tool Result Cache：
 * </p>
 *
 * <pre>
 * ToolAction + trusted ToolExecutionContext
 *              ↓
 *        ToolResultCache
 *        ├─ HIT
 *        │   ↓
 *        │ normalize
 *        │   ↓
 *        │ Result Security  ← 不能绕过
 *        │   ↓
 *        │ success
 *        │
 *        └─ MISS
 *            ↓
 *        issue tenant execution token
 *            ↓
 *           MCP
 *            ↓
 *        raw result
 *            ↓
 *        normalize
 *            ↓
 *        Result Security
 *            ↓
 *        success
 *            ↓
 *        cache raw result
 * </pre>
 *
 * <p>
 * Cache Key 只基于原始 ToolAction arguments + trusted tenant scope，
 * 运行时生成的 _executionToken 永远不进入 Cache Key。
 * </p>
 */
@Component
@ConditionalOnProperty(prefix = "app.agent", name = "legal-tool-mode", havingValue = "mcp")
public class McpLegalKnowledgeTool
                implements AgentTool {

        private static final Logger log = LoggerFactory.getLogger(
                        McpLegalKnowledgeTool.class);

        public static final String TOOL_NAME = LegalToolContract.SEARCH_LEGAL_KNOWLEDGE;

        private final ToolCallback toolCallback;

        private final ObjectMapper objectMapper;

        private final McpToolResultSecurityService resultSecurityService;

        private final SecurityAuditLogger securityAuditLogger;

        private final McpTenantExecutionTokenService tenantExecutionTokenService;

        private final ToolResultCache toolResultCache;

        private final ToolCachePolicy toolCachePolicy;

        private final CacheKeyFactory cacheKeyFactory;

        private final AiCacheProperties cacheProperties;

        private final LegalEvidenceCompactor legalEvidenceCompactor;

        private final AgentPerformanceContext performanceContext;

        public McpLegalKnowledgeTool(
                        SyncMcpToolCallbackProvider toolCallbackProvider,
                        ObjectMapper objectMapper,
                        McpToolResultSecurityService resultSecurityService,
                        SecurityAuditLogger securityAuditLogger,
                        McpTenantExecutionTokenService tenantExecutionTokenService,
                        ToolResultCache toolResultCache,
                        ToolCachePolicy toolCachePolicy,
                        CacheKeyFactory cacheKeyFactory,
                        AiCacheProperties cacheProperties,
                        LegalEvidenceCompactor legalEvidenceCompactor,
                        AgentPerformanceContext performanceContext) {

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

                this.tenantExecutionTokenService = Objects.requireNonNull(
                                tenantExecutionTokenService,
                                "tenantExecutionTokenService must not be null");

                this.toolResultCache = Objects.requireNonNull(
                                toolResultCache,
                                "toolResultCache must not be null");

                this.toolCachePolicy = Objects.requireNonNull(
                                toolCachePolicy,
                                "toolCachePolicy must not be null");

                this.cacheKeyFactory = Objects.requireNonNull(
                                cacheKeyFactory,
                                "cacheKeyFactory must not be null");

                this.cacheProperties = Objects.requireNonNull(
                                cacheProperties,
                                "cacheProperties must not be null");

                this.toolCallback = resolveToolCallback(
                                toolCallbackProvider);

                this.legalEvidenceCompactor = Objects.requireNonNull(
                                legalEvidenceCompactor,
                                "legalEvidenceCompactor must not be null");

                this.performanceContext = Objects.requireNonNull(
                                performanceContext,
                                "AgentPerformanceContext must not be null");
        }

        @Override
        public String name() {

                return TOOL_NAME;
        }

        /**
         * Legacy/internal path：没有可信 TenantContext 时只能按 SHARED scope 执行。
         */
        @Override
        public ToolExecutionResult execute(
                        ToolAction action) {

                return execute(
                                ToolExecutionContext.sharedOnly(),
                                action);
        }

        /**
         * Trusted runtime path。
         *
         * <p>
         * tenant identity 只能来自 ToolExecutionContext，
         * 绝不从 LLM-controlled ToolAction arguments 读取。
         * </p>
         */
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
                         * 原始参数属于 Cache Key 输入。
                         * 这里绝不能提前追加 _executionToken，
                         * 否则每次 token 不同会造成永远 MISS。
                         */
                        Map<String, Object> originalArguments = action.getArguments();

                        String cacheKey = resolveToolCacheKey(
                                        executionContext,
                                        action,
                                        originalArguments);

                        if (cacheKey != null) {

                                String cachedRawResult = toolResultCache.get(
                                                cacheKey)
                                                .orElse(
                                                                null);

                                if (cachedRawResult != null) {

                                        performanceContext
                                                        .current()
                                                        .ifPresent(
                                                                        metrics -> metrics.recordCacheHit());

                                        log.info(
                                                        "Tool result cache hit. toolName={}, scope={}",
                                                        TOOL_NAME,
                                                        resolveCacheScope(
                                                                        executionContext));

                                        return buildSuccessfulResult(
                                                        cachedRawResult);
                                }

                                performanceContext
                                                .current()
                                                .ifPresent(
                                                                metrics -> metrics.recordCacheMiss());

                                log.info(
                                                "Tool result cache miss. toolName={}, scope={}",
                                                TOOL_NAME,
                                                resolveCacheScope(
                                                                executionContext));
                        }

                        Map<String, Object> runtimeArguments = buildRuntimeArguments(
                                        executionContext,
                                        originalArguments);

                        String argumentsJson = serializeArguments(
                                        runtimeArguments);

                        PerformanceTimer timer = PerformanceTimer.start();

                        String rawResult;

                        try {

                                rawResult = toolCallback.call(
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
                                                TOOL_NAME,
                                                timer.elapsedMillis());
                        }

                        if (rawResult == null
                                        || rawResult.isBlank()) {

                                return ToolExecutionResult.failure(
                                                "MCP tool returned empty result");
                        }

                        ToolExecutionResult result = buildSuccessfulResult(
                                        rawResult);

                        /*
                         * 只有通过 Security Evaluation 的成功结果才写缓存。
                         * Security deny / timeout / 503 / runtime failure 都不会被缓存。
                         */
                        if (result.isSuccess()
                                        && cacheKey != null) {

                                toolResultCache.put(
                                                cacheKey,
                                                rawResult);

                                log.info(
                                                "Tool result cached. toolName={}, scope={}",
                                                TOOL_NAME,
                                                resolveCacheScope(
                                                                executionContext));
                        }

                        return result;

                } catch (McpToolResultSecurityViolationException exception) {

                        return ToolExecutionResult.failure(
                                        "MCP tool result was rejected by security policy");

                } catch (RuntimeException exception) {

                        /*
                         * 不向 Agent / LLM 暴露内部异常类型与下游基础设施细节。
                         */
                        return ToolExecutionResult.failure(
                                        "MCP tool execution failed");
                }
        }

        @Override
        public SecuritySource resultSecuritySource() {

                return SecuritySource.MCP_RESULT;
        }

        /**
         * Cache MISS 时构造真正发送给 MCP Server 的参数。
         *
         * <p>
         * Tenant execution token 是 server-side trusted data，
         * 只写入本次 runtime 参数副本，不修改 ToolAction.arguments。
         * </p>
         */
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

        /**
         * 统一处理 Cache HIT 和 MCP MISS 返回结果。
         *
         * <p>
         * 两条路径都必须执行：
         * normalize → result security → success。
         * </p>
         */
        private ToolExecutionResult buildSuccessfulResult(
                        String rawResult) {

                String normalizedResult = normalizeMcpResult(
                                rawResult);

                if (normalizedResult == null
                                || normalizedResult.isBlank()) {

                        return ToolExecutionResult.failure(
                                        "MCP tool returned empty result");
                }

                /*
                 * Security 必须检查原始 normalized Evidence。
                 *
                 * 不能：
                 * compact → security
                 *
                 * 否则恶意内容有可能恰好被 truncation 截掉，
                 * 从而绕过 MCP Result Security。
                 */
                McpToolResultSecurityResult securityResult = resultSecurityService.evaluate(
                                TOOL_NAME,
                                normalizedResult);

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

        /**
         * Tool Cache Key 只由：
         * scope + trusted tenantId + original Tool arguments + knowledgeVersion 构成。
         */
        private String resolveToolCacheKey(
                        ToolExecutionContext executionContext,
                        ToolAction action,
                        Map<String, Object> originalArguments) {

                if (!isToolCacheEnabled()
                                || !toolCachePolicy.isCacheable(
                                                action.getToolName())) {

                        return null;
                }

                CacheScope scope = resolveCacheScope(
                                executionContext);

                String tenantId = scope == CacheScope.TENANT
                                ? executionContext
                                                .requireTenantContext()
                                                .tenantId()
                                : null;

                Map<String, Object> cacheArguments = resolveToolCacheArguments(
                                executionContext,
                                action,
                                originalArguments);

                return cacheKeyFactory.toolKey(
                                scope,
                                tenantId,
                                action.getToolName(),
                                cacheArguments,
                                cacheProperties.getKnowledgeVersion());
        }

        private CacheScope resolveCacheScope(
                        ToolExecutionContext executionContext) {

                return executionContext.hasTenantContext()
                                ? CacheScope.TENANT
                                : CacheScope.SHARED;
        }

        private boolean isToolCacheEnabled() {

                return cacheProperties.isEnabled()
                                && cacheProperties
                                                .getTool()
                                                .isEnabled();
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
                         * MCP Callback 如果直接返回普通文本，
                         * 不做 JSON 转换，原样返回。
                         */
                }

                return normalized;
        }

        private Map<String, Object> resolveToolCacheArguments(
                        ToolExecutionContext executionContext,
                        ToolAction action,
                        Map<String, Object> originalArguments) {

                /*
                 * Legal search cache identity is based on the
                 * trusted original Agent Goal.
                 *
                 * Planner-generated legalQuestion is intentionally
                 * excluded because its wording is nondeterministic
                 * across equivalent executions.
                 */
                if (TOOL_NAME.equals(
                                action.getToolName())
                                && executionContext.hasExecutionGoal()) {

                        return Map.of(
                                        "goal",
                                        executionContext.requireExecutionGoal());
                }

                /*
                 * Legacy / internal path remains backward compatible.
                 */
                return originalArguments == null
                                ? Map.of()
                                : originalArguments;
        }
}