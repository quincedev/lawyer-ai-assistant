package com.quince.lawyeraiassistant.rag.embedding;

import com.quince.lawyeraiassistant.rag.config.ConditionalOnRagEnabled;
import com.quince.lawyeraiassistant.rag.embedding.dto.EmbeddingPreviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnRagEnabled
public class DocumentEmbeddingService {

    private static final int PREVIEW_SIZE = 8;

    private final EmbeddingModel embeddingModel;

    /**
     * 将单段文本转换为向量。
     */
    public float[] embedText(String text) {

        validateText(text);

        return embeddingModel.embed(text);
    }

    /**
     * 将单个 Document 转换为向量。
     */
    public float[] embedDocument(Document document) {

        if (document == null) {
            throw new IllegalArgumentException(
                    "document 不能为空");
        }

        if (document.getText() == null
                || document.getText().isBlank()) {

            throw new IllegalArgumentException(
                    "document text 不能为空");
        }

        return embeddingModel.embed(document);
    }

    /**
     * 批量转换文本。
     */
    public List<float[]> embedTexts(List<String> texts) {

        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        texts.forEach(this::validateText);

        return embeddingModel.embed(texts);
    }

    /**
     * 获取完整的 Spring AI 响应对象。
     */
    public EmbeddingResponse embedForResponse(
            List<String> texts) {

        if (texts == null || texts.isEmpty()) {
            throw new IllegalArgumentException(
                    "texts 不能为空");
        }

        texts.forEach(this::validateText);

        return embeddingModel.embedForResponse(texts);
    }

    /**
     * 生成适合接口查看的向量预览。
     */
    public EmbeddingPreviewResponse preview(String text) {

        float[] vector = embedText(text);

        return new EmbeddingPreviewResponse(
                text,
                vector.length,
                extractPreview(vector));
    }

    /**
     * 解析完整响应中的每个 Embedding。
     */
    public List<EmbeddingPreviewResponse> previewBatch(
            List<String> texts) {

        EmbeddingResponse response = embedForResponse(texts);

        List<Embedding> results = response.getResults();

        List<EmbeddingPreviewResponse> previews = new ArrayList<>(results.size());

        for (Embedding result : results) {

            int index = result.getIndex();
            float[] vector = result.getOutput();

            previews.add(
                    new EmbeddingPreviewResponse(
                            texts.get(index),
                            vector.length,
                            extractPreview(vector)));
        }

        return previews;
    }

    private List<Float> extractPreview(float[] vector) {

        int previewLength = Math.min(PREVIEW_SIZE, vector.length);

        List<Float> preview = new ArrayList<>(previewLength);

        for (int index = 0; index < previewLength; index++) {

            preview.add(vector[index]);
        }

        return preview;
    }

    private void validateText(String text) {

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(
                    "待向量化文本不能为空");
        }
    }
}