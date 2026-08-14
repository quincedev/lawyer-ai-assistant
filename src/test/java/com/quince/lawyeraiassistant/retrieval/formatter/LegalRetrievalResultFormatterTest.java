package com.quince.lawyeraiassistant.retrieval.formatter;

import com.quince.lawyeraiassistant.query.model.QueryContext;
import com.quince.lawyeraiassistant.retrieval.model.RetrieverContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegalRetrievalResultFormatterTest {

    private LegalRetrievalResultFormatter formatter;

    @BeforeEach
    void setUp() {

        formatter = new LegalRetrievalResultFormatter();
    }

    @Test
    void shouldFormatRetrievalResult() {

        QueryContext queryContext = QueryContext.builder()
                .question(
                        "违法解除劳动合同有什么责任")
                .rewriteQuery(
                        "违法解除劳动合同赔偿责任")
                .build();

        Document firstDocument = new Document(
                "doc-1",
                "第八十七条规定，用人单位违法解除劳动合同的，应当支付赔偿金。",
                Map.of(
                        "file_name",
                        "Labor-Contract-Law.pdf",
                        "page_number",
                        24,
                        "chunk_index",
                        3));

        Document secondDocument = new Document(
                "doc-2",
                "第四十七条规定了经济补偿的计算标准。",
                Map.of(
                        "file_name",
                        "Labor-Contract-Law.pdf",
                        "page_number",
                        14,
                        "chunk_index",
                        1));

        RetrieverContext context = RetrieverContext.builder()
                .queryContext(
                        queryContext)
                .documents(
                        List.of(
                                firstDocument,
                                secondDocument))
                .build();

        String result = formatter.format(
                context);

        assertTrue(
                result.contains(
                        "有效检索问题：违法解除劳动合同赔偿责任"));

        assertTrue(
                result.contains(
                        "检索文档数量：2"));

        assertTrue(
                result.contains(
                        "参考资料 1"));

        assertTrue(
                result.contains(
                        "参考资料 2"));

        assertTrue(
                result.contains(
                        "来源：Labor-Contract-Law.pdf"));

        assertTrue(
                result.contains(
                        "页码：24"));

        assertTrue(
                result.contains(
                        "Chunk：3"));

        assertTrue(
                result.contains(
                        "第八十七条"));

        assertTrue(
                result.contains(
                        "第四十七条"));

        assertTrue(
                result.contains(
                        "---"));
    }

    @Test
    void shouldUseRewriteQueryAsEffectiveQuery() {

        RetrieverContext context = RetrieverContext.builder()
                .queryContext(
                        QueryContext.builder()
                                .question(
                                        "老板把我开了怎么办")
                                .rewriteQuery(
                                        "违法解除劳动合同法律责任")
                                .build())
                .documents(
                        List.of(
                                new Document(
                                        "测试法律内容")))
                .build();

        String result = formatter.format(
                context);

        assertTrue(
                result.contains(
                        "有效检索问题：违法解除劳动合同法律责任"));
    }

    @Test
    void shouldUseOriginalQuestionWhenRewriteQueryDoesNotExist() {

        RetrieverContext context = RetrieverContext.builder()
                .queryContext(
                        QueryContext.from(
                                "劳动合同解除条件"))
                .documents(
                        List.of(
                                new Document(
                                        "测试法律内容")))
                .build();

        String result = formatter.format(
                context);

        assertTrue(
                result.contains(
                        "有效检索问题：劳动合同解除条件"));
    }

    @Test
    void shouldReturnNoKnowledgeMessageWhenNoDocumentsFound() {

        RetrieverContext context = RetrieverContext.from(
                QueryContext.from(
                        "知识库不存在的问题"));

        String result = formatter.format(
                context);

        assertEquals(
                "未检索到与当前法律问题相关的知识。",
                result);
    }

    @Test
    void shouldIgnoreMissingMetadata() {

        RetrieverContext context = RetrieverContext.builder()
                .queryContext(
                        QueryContext.from(
                                "劳动合同问题"))
                .documents(
                        List.of(
                                new Document(
                                        "法律内容")))
                .build();

        String result = formatter.format(
                context);

        assertTrue(
                result.contains(
                        "法律内容"));

        assertFalse(
                result.contains(
                        "来源："));

        assertFalse(
                result.contains(
                        "页码："));

        assertFalse(
                result.contains(
                        "Chunk："));
    }

    @Test
    void shouldIgnoreBlankMetadataValue() {

        Document document = new Document(
                "doc-1",
                "法律内容",
                Map.of(
                        "file_name",
                        "   ",
                        "page_number",
                        10));

        RetrieverContext context = RetrieverContext.builder()
                .queryContext(
                        QueryContext.from(
                                "劳动合同问题"))
                .documents(
                        List.of(
                                document))
                .build();

        String result = formatter.format(
                context);

        assertFalse(
                result.contains(
                        "来源："));

        assertTrue(
                result.contains(
                        "页码：10"));
    }

    @Test
    void shouldFormatMultipleDocumentsInOrder() {

        Document firstDocument = new Document(
                "第一份法律资料");

        Document secondDocument = new Document(
                "第二份法律资料");

        RetrieverContext context = RetrieverContext.builder()
                .queryContext(
                        QueryContext.from(
                                "测试问题"))
                .documents(
                        List.of(
                                firstDocument,
                                secondDocument))
                .build();

        String result = formatter.format(
                context);

        int firstIndex = result.indexOf(
                "第一份法律资料");

        int secondIndex = result.indexOf(
                "第二份法律资料");

        assertTrue(
                firstIndex >= 0);

        assertTrue(
                secondIndex > firstIndex);
    }

    @Test
    void shouldRejectNullRetrieverContext() {

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> formatter.format(
                        null));

        assertEquals(
                "RetrieverContext must not be null",
                exception.getMessage());
    }
}