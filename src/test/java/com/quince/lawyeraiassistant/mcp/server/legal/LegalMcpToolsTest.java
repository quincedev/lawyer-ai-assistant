package com.quince.lawyeraiassistant.mcp.server.legal;

import com.quince.lawyeraiassistant.query.model.QueryContext;
import com.quince.lawyeraiassistant.retrieval.formatter.LegalRetrievalResultFormatter;
import com.quince.lawyeraiassistant.retrieval.model.RetrieverContext;
import com.quince.lawyeraiassistant.retrieval.orchestration.RetrievalOrchestrator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LegalMcpToolsTest {

    private RetrievalOrchestrator retrievalOrchestrator;

    private LegalRetrievalResultFormatter resultFormatter;

    private LegalMcpTools legalMcpTools;

    @BeforeEach
    void setUp() {

        retrievalOrchestrator = mock(
                RetrievalOrchestrator.class);

        resultFormatter = new LegalRetrievalResultFormatter();

        legalMcpTools = new LegalMcpTools(
                retrievalOrchestrator,
                resultFormatter);
    }

    @Test
    void shouldSearchLegalKnowledge() {

        QueryContext queryContext = QueryContext.builder()
                .question(
                        "违法解除劳动合同有什么责任")
                .rewriteQuery(
                        "违法解除劳动合同赔偿责任")
                .build();

        Document document = new Document(
                "doc-1",
                "第八十七条规定，用人单位违法解除劳动合同，应当支付赔偿金。",
                Map.of(
                        "file_name",
                        "Labor-Contract-Law.pdf",
                        "page_number",
                        24));

        RetrieverContext retrievalContext = RetrieverContext.builder()
                .queryContext(
                        queryContext)
                .documents(
                        List.of(
                                document))
                .build();

        when(
                retrievalOrchestrator.retrieve(
                        "违法解除劳动合同有什么责任"))
                .thenReturn(
                        retrievalContext);

        String result = legalMcpTools.searchLegalKnowledge(
                "违法解除劳动合同有什么责任");

        assertTrue(
                result.contains(
                        "有效检索问题：违法解除劳动合同赔偿责任"));

        assertTrue(
                result.contains(
                        "第八十七条"));

        assertTrue(
                result.contains(
                        "Labor-Contract-Law.pdf"));

        verify(
                retrievalOrchestrator)
                .retrieve(
                        "违法解除劳动合同有什么责任");
    }

    @Test
    void shouldTrimLegalQuestionBeforeRetrieval() {

        RetrieverContext retrievalContext = RetrieverContext.from(
                QueryContext.from(
                        "劳动合同解除条件"));

        when(
                retrievalOrchestrator.retrieve(
                        "劳动合同解除条件"))
                .thenReturn(
                        retrievalContext);

        String result = legalMcpTools.searchLegalKnowledge(
                "   劳动合同解除条件   ");

        assertEquals(
                "未检索到与当前法律问题相关的知识。",
                result);

        verify(
                retrievalOrchestrator)
                .retrieve(
                        "劳动合同解除条件");
    }

    @Test
    void shouldReturnNoKnowledgeMessageWhenNoDocumentsFound() {

        RetrieverContext retrievalContext = RetrieverContext.from(
                QueryContext.from(
                        "不存在的法律问题"));

        when(
                retrievalOrchestrator.retrieve(
                        "不存在的法律问题"))
                .thenReturn(
                        retrievalContext);

        String result = legalMcpTools.searchLegalKnowledge(
                "不存在的法律问题");

        assertEquals(
                "未检索到与当前法律问题相关的知识。",
                result);
    }

    @Test
    void shouldPropagateRetrievalException() {

        when(
                retrievalOrchestrator.retrieve(
                        "劳动合同解除条件"))
                .thenThrow(
                        new IllegalStateException(
                                "VectorStore unavailable"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> legalMcpTools.searchLegalKnowledge(
                        "劳动合同解除条件"));

        assertEquals(
                "VectorStore unavailable",
                exception.getMessage());
    }

    @Test
    void shouldRejectNullRetrievalResult() {

        when(
                retrievalOrchestrator.retrieve(
                        "劳动合同解除条件"))
                .thenReturn(
                        null);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> legalMcpTools.searchLegalKnowledge(
                        "劳动合同解除条件"));

        assertEquals(
                "RetrievalOrchestrator must not return null",
                exception.getMessage());
    }

    @Test
    void shouldRejectNullLegalQuestion() {

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> legalMcpTools.searchLegalKnowledge(
                        null));

        assertEquals(
                "Legal question must not be null",
                exception.getMessage());

        verify(
                retrievalOrchestrator,
                never())
                .retrieve(
                        org.mockito.ArgumentMatchers
                                .anyString());
    }

    @Test
    void shouldRejectBlankLegalQuestion() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> legalMcpTools.searchLegalKnowledge(
                        "   "));

        assertEquals(
                "Legal question must not be blank",
                exception.getMessage());

        verify(
                retrievalOrchestrator,
                never())
                .retrieve(
                        org.mockito.ArgumentMatchers
                                .anyString());
    }

    @Test
    void shouldRejectNullRetrievalOrchestrator() {

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new LegalMcpTools(
                        null,
                        resultFormatter));

        assertEquals(
                "retrievalOrchestrator must not be null",
                exception.getMessage());
    }

    @Test
    void shouldRejectNullResultFormatter() {

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new LegalMcpTools(
                        retrievalOrchestrator,
                        null));

        assertEquals(
                "resultFormatter must not be null",
                exception.getMessage());
    }
}