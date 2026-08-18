package com.quince.lawyeraiassistant.agent.tool.legal;

import com.quince.lawyeraiassistant.agent.model.ToolAction;
import com.quince.lawyeraiassistant.agent.model.ToolExecutionResult;
import com.quince.lawyeraiassistant.agent.tool.AgentTool;
import com.quince.lawyeraiassistant.agent.tool.ToolExecutionContext;
import com.quince.lawyeraiassistant.retrieval.formatter.LegalRetrievalResultFormatter;
import com.quince.lawyeraiassistant.retrieval.model.RetrieverContext;
import com.quince.lawyeraiassistant.retrieval.orchestration.RetrievalOrchestrator;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * 法律知识检索 Agent Tool。
 *
 * <p>
 * 将 Agent Runtime 中的法律知识检索 Action
 * 适配到 RetrievalOrchestrator。
 * </p>
 *
 * <pre>
 * ToolAction
 *      ↓
 * LegalKnowledgeTool
 *      ↓
 * RetrievalOrchestrator
 *      ↓
 * LegalRetrievalResultFormatter
 *      ↓
 * ToolExecutionResult
 * </pre>
 *
 * <p>
 * 本类只作为 Agent Tool Adapter，
 * 不实现具体 Retrieval 逻辑，
 * 也不负责 MCP 协议。
 * </p>
 */
@Component
@ConditionalOnProperty(prefix = "app.agent", name = "legal-tool-mode", havingValue = "local", matchIfMissing = true)
public class LegalKnowledgeTool
                implements AgentTool {

        public static final String TOOL_NAME = LegalToolContract.SEARCH_LEGAL_KNOWLEDGE;

        public static final String LEGAL_QUESTION_ARGUMENT = LegalToolContract.LEGAL_QUESTION;

        private final RetrievalOrchestrator retrievalOrchestrator;

        private final LegalRetrievalResultFormatter resultFormatter;

        public LegalKnowledgeTool(
                        RetrievalOrchestrator retrievalOrchestrator,
                        LegalRetrievalResultFormatter resultFormatter) {

                this.retrievalOrchestrator = Objects.requireNonNull(
                                retrievalOrchestrator,
                                "retrievalOrchestrator must not be null");

                this.resultFormatter = Objects.requireNonNull(
                                resultFormatter,
                                "resultFormatter must not be null");
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

                String legalQuestion = extractLegalQuestion(
                                action.getArguments());

                try {

                        RetrieverContext retrievalContext;

                        if (executionContext.hasTenantContext()) {

                                String tenantId = executionContext
                                                .requireTenantContext()
                                                .tenantId();

                                retrievalContext = retrievalOrchestrator
                                                .retrieveForTenant(
                                                                legalQuestion,
                                                                tenantId);

                        } else {

                                /*
                                 * Legacy/internal call:
                                 * Step 5 guarantees retrieve(...)
                                 * is SHARED-only.
                                 */
                                retrievalContext = retrievalOrchestrator.retrieve(
                                                legalQuestion);
                        }

                        Objects.requireNonNull(
                                        retrievalContext,
                                        "RetrievalOrchestrator must not return null");

                        return ToolExecutionResult.success(
                                        resultFormatter.format(
                                                        retrievalContext));

                } catch (RuntimeException exception) {

                        return ToolExecutionResult.failure(
                                        resolveErrorMessage(
                                                        exception));
                }
        }

        /**
         * 防止 Runtime 将其他 ToolAction
         * 错误路由到当前 Tool。
         */
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

        /**
         * 提取 Tool 参数中的法律问题。
         */
        private String extractLegalQuestion(
                        Map<String, Object> arguments) {

                Object rawQuestion = arguments.get(
                                LEGAL_QUESTION_ARGUMENT);

                if (rawQuestion == null) {

                        throw new IllegalArgumentException(
                                        "Missing required tool argument: "
                                                        + LEGAL_QUESTION_ARGUMENT);
                }

                String legalQuestion = rawQuestion.toString()
                                .trim();

                if (legalQuestion.isEmpty()) {

                        throw new IllegalArgumentException(
                                        "Tool argument legalQuestion must not be blank");
                }

                return legalQuestion;
        }

        /**
         * 将 Retrieval 异常转换为 Agent Tool Failure。
         */
        private String resolveErrorMessage(
                        RuntimeException exception) {

                String message = exception.getMessage();

                if (message == null
                                || message.isBlank()) {

                        return exception.getClass()
                                        .getSimpleName();
                }

                return message.trim();
        }
}