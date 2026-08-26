package com.quince.lawyeraiassistant.rag.splitter;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import com.quince.lawyeraiassistant.rag.config.ConditionalOnRagEnabled;

import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnRagEnabled
public class LaborLawSplitter {

    private final LegalTextSplitter legalTextSplitter;

    public List<Document> split(List<Document> documents) {

        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        return legalTextSplitter.apply(documents);
    }
}