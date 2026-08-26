package com.quince.lawyeraiassistant.controller;

import com.quince.lawyeraiassistant.rag.config.ConditionalOnRagEnabled;
import com.quince.lawyeraiassistant.retrieval.model.RetrieverContext;
import com.quince.lawyeraiassistant.retrieval.orchestration.RetrievalOrchestrator;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 自定义 Retriever Pipeline 的开发诊断接口。
 *
 * <p>
 * 仅用于验证 Query Pipeline 和 Retriever Pipeline，
 * 不属于正式律师问答接口。
 * </p>
 */
@RestController
@ConditionalOnRagEnabled
@RequestMapping("/api/playground/retrieval")
public class RetrievalPlaygroundController {

    private final RetrievalOrchestrator retrievalOrchestrator;

    public RetrievalPlaygroundController(
            RetrievalOrchestrator retrievalOrchestrator
    ) {
        this.retrievalOrchestrator =
                retrievalOrchestrator;
    }

    @GetMapping
    public Map<String, Object> retrieve(
            @RequestParam String question,
            @RequestParam(required = false)
            String conversationId
    ) {
        RetrieverContext context =
                retrievalOrchestrator.retrieve(
                        question,
                        conversationId
                );

        List<Map<String, Object>> documents =
                context.getDocuments()
                        .stream()
                        .map(this::toDocumentView)
                        .toList();

        return Map.of(
                "question",
                context.getQueryContext()
                        .getQuestion(),
                "effectiveQuery",
                context.effectiveQuery(),
                "documentCount",
                context.documentCount(),
                "documents",
                documents
        );
    }

    private Map<String, Object> toDocumentView(
            Document document
    ) {
        return Map.of(
                "id",
                document.getId(),
                "text",
                document.getText(),
                "metadata",
                document.getMetadata()
        );
    }
}
