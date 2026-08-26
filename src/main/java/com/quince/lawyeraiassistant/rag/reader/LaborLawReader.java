package com.quince.lawyeraiassistant.rag.reader;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.quince.lawyeraiassistant.rag.config.ConditionalOnRagEnabled;

import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnRagEnabled
public class LaborLawReader {

    private final Resource laborLawPdf;

    public List<Document> read() {

        PagePdfDocumentReader reader = new PagePdfDocumentReader(laborLawPdf);

        return reader.get();

    }

}