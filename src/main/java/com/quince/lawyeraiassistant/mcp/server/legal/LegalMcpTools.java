package com.quince.lawyeraiassistant.mcp.server.legal;

import com.quince.lawyeraiassistant.agent.tool.legal.LegalToolContract;
import com.quince.lawyeraiassistant.retrieval.formatter.LegalRetrievalResultFormatter;
import com.quince.lawyeraiassistant.retrieval.model.RetrieverContext;
import com.quince.lawyeraiassistant.retrieval.orchestration.RetrievalOrchestrator;

import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

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
public class LegalMcpTools {

    private final RetrievalOrchestrator retrievalOrchestrator;

    private final LegalRetrievalResultFormatter resultFormatter;

    public LegalMcpTools(
            RetrievalOrchestrator retrievalOrchestrator,
            LegalRetrievalResultFormatter resultFormatter) {

        this.retrievalOrchestrator = Objects.requireNonNull(
                retrievalOrchestrator,
                "retrievalOrchestrator must not be null");

        this.resultFormatter = Objects.requireNonNull(
                resultFormatter,
                "resultFormatter must not be null");
    }

    /**
     * MCP Tool：
     * 检索中国法律知识库。
     */
    @McpTool(name = LegalToolContract.SEARCH_LEGAL_KNOWLEDGE, description = "检索中国法律知识库，为法律问题分析提供相关法律条文和参考资料")
    public String searchLegalKnowledge(
            @McpToolParam(description = "需要检索法律依据的法律问题", required = true) String legalQuestion) {

        log.info(
                "MCP tool invoked: tool=searchLegalKnowledge, legalQuestion={}",
                legalQuestion);
        String normalizedQuestion = normalizeLegalQuestion(
                legalQuestion);

        RetrieverContext retrievalContext = retrievalOrchestrator.retrieve(
                normalizedQuestion);

        Objects.requireNonNull(
                retrievalContext,
                "RetrievalOrchestrator must not return null");

        return resultFormatter.format(
                retrievalContext);
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
}