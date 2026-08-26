package com.quince.lawyeraiassistant.rag.controller;

import com.quince.lawyeraiassistant.rag.config.ConditionalOnRagEnabled;
import com.quince.lawyeraiassistant.rag.dto.ChunkPreviewResponse;
import com.quince.lawyeraiassistant.rag.service.DocumentLoadService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@ConditionalOnRagEnabled
@RequestMapping("/rag")
@RequiredArgsConstructor
public class RagController {

    private final DocumentLoadService documentLoadService;

    @GetMapping("/documents")
    public List<Document> documents() {
        return documentLoadService.loadDocuments();
    }

    @GetMapping("/chunks")
    public List<Document> chunks() {
        return documentLoadService.loadChunks();
    }

    @GetMapping("/chunks/preview")
    public List<ChunkPreviewResponse> previewChunks() {
        return documentLoadService.previewChunks();
    }
}
