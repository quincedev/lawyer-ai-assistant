package com.quince.lawyeraiassistant.rag.service;

import com.quince.lawyeraiassistant.rag.config.ConditionalOnRagEnabled;
import com.quince.lawyeraiassistant.rag.dto.ChunkPreviewResponse;
import com.quince.lawyeraiassistant.rag.pipeline.LaborLawDocumentPipeline;
import com.quince.lawyeraiassistant.rag.reader.LaborLawReader;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnRagEnabled
public class DocumentLoadService {

    private final LaborLawReader laborLawReader;

    private final LaborLawDocumentPipeline laborLawDocumentPipeline;

    /**
     * 查看 Reader 原始读取结果，仅用于调试和学习。
     */
    public List<Document> loadDocuments() {
        return laborLawReader.read();
    }

    /**
     * 执行完整文档预处理流水线。
     */
    public List<Document> loadChunks() {
        return laborLawDocumentPipeline.process();
    }

    public List<ChunkPreviewResponse> previewChunks() {

        List<Document> chunks = loadChunks();

        List<ChunkPreviewResponse> responses = new ArrayList<>(chunks.size());

        for (int index = 0; index < chunks.size(); index++) {

            Document chunk = chunks.get(index);
            String text = chunk.getText();

            responses.add(
                    new ChunkPreviewResponse(
                            index,
                            text == null ? 0 : text.length(),
                            text,
                            chunk.getMetadata()));
        }

        return responses;
    }
}