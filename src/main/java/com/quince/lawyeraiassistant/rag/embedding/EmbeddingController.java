package com.quince.lawyeraiassistant.rag.embedding;

import com.quince.lawyeraiassistant.rag.config.ConditionalOnRagEnabled;
import com.quince.lawyeraiassistant.rag.embedding.dto.EmbeddingPreviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnRagEnabled
@RequestMapping("/rag/embeddings")
@RequiredArgsConstructor
public class EmbeddingController {

    private final DocumentEmbeddingService embeddingService;

    @GetMapping("/preview")
    public EmbeddingPreviewResponse preview(
            @RequestParam String text) {

        return embeddingService.preview(text);
    }
}
