package com.quince.lawyeraiassistant.rag.pipeline;

import com.quince.lawyeraiassistant.rag.config.ConditionalOnRagEnabled;
import com.quince.lawyeraiassistant.rag.reader.LaborLawReader;
import com.quince.lawyeraiassistant.rag.splitter.LaborLawSplitter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnRagEnabled
public class LaborLawDocumentPipeline implements DocumentPipeline {

    private final LaborLawReader laborLawReader;

    private final LaborLawSplitter laborLawSplitter;

    @Override
    public List<Document> process() {

        log.info("开始执行劳动合同法文档处理流水线");

        List<Document> sourceDocuments = laborLawReader.read();

        if (sourceDocuments == null
                || sourceDocuments.isEmpty()) {

            log.warn("未读取到任何劳动合同法文档");
            return List.of();
        }

        List<Document> chunks = laborLawSplitter.split(sourceDocuments);

        List<Document> validChunks = filterInvalidChunks(chunks);

        List<Document> enrichedChunks = enrichMetadata(validChunks);

        log.info(
                "文档处理完成，原始文档：{}，切分结果：{}，有效结果：{}",
                sourceDocuments.size(),
                chunks.size(),
                enrichedChunks.size());

        return enrichedChunks;
    }

    private List<Document> filterInvalidChunks(
            List<Document> chunks) {

        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }

        return chunks.stream()
                .filter(document -> document != null)
                .filter(document -> document.getText() != null)
                .filter(document -> !document.getText().isBlank())
                .toList();
    }

    private List<Document> enrichMetadata(
            List<Document> documents) {

        List<Document> enrichedDocuments = new ArrayList<>(documents.size());

        for (int index = 0; index < documents.size(); index++) {

            Document source = documents.get(index);

            Map<String, Object> metadata = new HashMap<>(source.getMetadata());

            metadata.put(
                    "document_type",
                    "labor_law");

            metadata.put(
                    "knowledge_base",
                    "legal");

            metadata.put(
                    "pipeline",
                    "labor-law");

            metadata.put(
                    "chunk_index",
                    index);

            enrichedDocuments.add(
                    new Document(
                            source.getText(),
                            metadata));
        }

        return enrichedDocuments;
    }
}