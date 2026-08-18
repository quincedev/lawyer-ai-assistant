package com.quince.lawyeraiassistant.retrieval.model;

import com.quince.lawyeraiassistant.query.model.QueryContext;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetrieverContextTest {

        @Test
        void shouldCreateInitialContextFromQueryContext() {
                QueryContext queryContext = QueryContext.from(
                                "老板把我开了合法吗？");

                RetrieverContext context = RetrieverContext.from(queryContext);

                assertEquals(
                                queryContext,
                                context.getQueryContext());

                assertTrue(context.getDocuments().isEmpty());
                assertFalse(context.hasDocuments());
                assertEquals(0, context.documentCount());
        }

        @Test
        void shouldReturnOriginalQuestionBeforeRewrite() {
                QueryContext queryContext = QueryContext.from(
                                "老板不给工资怎么办？");

                RetrieverContext context = RetrieverContext.from(queryContext);

                assertEquals(
                                "老板不给工资怎么办？",
                                context.effectiveQuery());
        }

        @Test
        void shouldReturnRewriteQueryAfterRewrite() {
                QueryContext queryContext = QueryContext.builder()
                                .question(
                                                "老板不给工资怎么办？")
                                .rewriteQuery(
                                                "拖欠劳动报酬的法律救济")
                                .build();

                RetrieverContext context = RetrieverContext.from(queryContext);

                assertEquals(
                                "拖欠劳动报酬的法律救济",
                                context.effectiveQuery());
        }

        @Test
        void shouldCreateContextWithDocuments() {
                QueryContext queryContext = QueryContext.from(
                                "劳动合同解除需要赔偿吗？");

                Document firstDocument = new Document(
                                "劳动合同法第四十六条。");

                Document secondDocument = new Document(
                                "劳动合同法第四十七条。");

                RetrieverContext context = RetrieverContext.builder()
                                .queryContext(queryContext)
                                .documents(
                                                List.of(
                                                                firstDocument,
                                                                secondDocument))
                                .build();

                assertTrue(context.hasDocuments());
                assertEquals(2, context.documentCount());

                assertEquals(
                                firstDocument,
                                context.getDocuments().get(0));

                assertEquals(
                                secondDocument,
                                context.getDocuments().get(1));
        }

        @Test
        void shouldNormalizeNullDocumentsToEmptyList() {
                QueryContext queryContext = QueryContext.from(
                                "劳动合同问题");

                RetrieverContext context = RetrieverContext.builder()
                                .queryContext(queryContext)
                                .documents(null)
                                .build();

                assertTrue(context.getDocuments().isEmpty());
                assertFalse(context.hasDocuments());
                assertEquals(0, context.documentCount());
        }

        @Test
        void shouldCreateNewContextWhenAddingDocuments() {
                QueryContext queryContext = QueryContext.from(
                                "竞业协议合法吗？");

                RetrieverContext originalContext = RetrieverContext.from(queryContext);

                Document document = new Document(
                                "劳动合同法第二十三条。");

                RetrieverContext retrievedContext = originalContext.toBuilder()
                                .documents(
                                                List.of(document))
                                .build();

                assertNotSame(
                                originalContext,
                                retrievedContext);

                assertFalse(originalContext.hasDocuments());
                assertEquals(0, originalContext.documentCount());

                assertTrue(retrievedContext.hasDocuments());
                assertEquals(1, retrievedContext.documentCount());
        }

        @Test
        void shouldPreserveQueryContextWhenAddingDocuments() {
                QueryContext queryContext = QueryContext.from(
                                "那赔偿呢？",
                                "conversation-001");

                RetrieverContext originalContext = RetrieverContext.from(queryContext);

                RetrieverContext retrievedContext = originalContext.toBuilder()
                                .documents(
                                                List.of(
                                                                new Document(
                                                                                "经济补偿相关规定。")))
                                .build();

                assertEquals(
                                queryContext,
                                retrievedContext.getQueryContext());

                assertEquals(
                                "conversation-001",
                                retrievedContext
                                                .getQueryContext()
                                                .getConversationId());
        }

        @Test
        void shouldCreateDefensiveCopyOfDocuments() {
                QueryContext queryContext = QueryContext.from(
                                "劳动合同到期怎么办？");

                List<Document> mutableDocuments = new ArrayList<>();

                mutableDocuments.add(
                                new Document(
                                                "劳动合同终止相关规定。"));

                RetrieverContext context = RetrieverContext.builder()
                                .queryContext(queryContext)
                                .documents(mutableDocuments)
                                .build();

                mutableDocuments.clear();

                assertEquals(1, context.documentCount());
                assertTrue(context.hasDocuments());
        }

        @Test
        void shouldExposeUnmodifiableDocumentList() {
                RetrieverContext context = RetrieverContext.builder()
                                .queryContext(
                                                QueryContext.from(
                                                                "测试问题"))
                                .documents(
                                                List.of(
                                                                new Document(
                                                                                "测试知识")))
                                .build();

                assertThrows(
                                UnsupportedOperationException.class,
                                () -> context
                                                .getDocuments()
                                                .add(
                                                                new Document(
                                                                                "非法新增知识")));
        }

        @Test
        void shouldRejectNullDocumentElement() {
                List<Document> documents = new ArrayList<>();

                documents.add(
                                new Document(
                                                "有效知识"));

                documents.add(null);

                assertThrows(
                                NullPointerException.class,
                                () -> RetrieverContext.builder()
                                                .queryContext(
                                                                QueryContext.from(
                                                                                "测试问题"))
                                                .documents(documents)
                                                .build());
        }

        @Test
        void shouldThrowExceptionWhenQueryContextIsNull() {
                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> RetrieverContext.from(null));

                assertEquals(
                                "QueryContext must not be null",
                                exception.getMessage());
        }

        @Test
        void shouldSupportEqualsAndHashCode() {
                QueryContext queryContext = QueryContext.from(
                                "劳动合同解除是否合法？");

                Document document = new Document(
                                "劳动合同解除相关规定。");

                RetrieverContext first = RetrieverContext.builder()
                                .queryContext(queryContext)
                                .documents(
                                                List.of(document))
                                .build();

                RetrieverContext second = RetrieverContext.builder()
                                .queryContext(queryContext)
                                .documents(
                                                List.of(document))
                                .build();

                assertEquals(first, second);

                assertEquals(
                                first.hashCode(),
                                second.hashCode());
        }

        @Test
        void shouldCreateTenantAwareRetrieverContext() {

                RetrieverContext context = RetrieverContext.tenantAware(
                                QueryContext.from(
                                                "劳动合同问题"),
                                " tenant-a ");

                assertTrue(
                                context.hasTenantId());

                assertEquals(
                                "tenant-a",
                                context.requireTenantId());
        }

        @Test
        void shouldRejectBlankTenantIdForTenantAwareContext() {

                assertThrows(
                                IllegalArgumentException.class,
                                () -> RetrieverContext.tenantAware(
                                                QueryContext.from(
                                                                "劳动合同问题"),
                                                " "));
        }

        @Test
        void shouldFailWhenTenantIdIsRequiredFromLegacyContext() {

                RetrieverContext context = RetrieverContext.from(
                                QueryContext.from(
                                                "劳动合同问题"));

                assertFalse(
                                context.hasTenantId());

                assertThrows(
                                IllegalStateException.class,
                                context::requireTenantId);
        }
}