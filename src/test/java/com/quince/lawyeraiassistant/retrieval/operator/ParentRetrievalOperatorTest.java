package com.quince.lawyeraiassistant.retrieval.operator;

import com.quince.lawyeraiassistant.query.model.QueryContext;
import com.quince.lawyeraiassistant.rag.vector.tenant.TenantKnowledgeAccessPolicy;
import com.quince.lawyeraiassistant.retrieval.model.RetrieverContext;
import com.quince.lawyeraiassistant.retrieval.parent.provider.ParentDocumentProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ParentRetrievalOperatorTest {

        private ParentDocumentProvider parentDocumentProvider;

        private ParentRetrievalOperator operator;

        private TenantKnowledgeAccessPolicy tenantKnowledgeAccessPolicy;

        @BeforeEach
        void setUp() {

                parentDocumentProvider = mock(
                                ParentDocumentProvider.class);

                tenantKnowledgeAccessPolicy = mock(
                                TenantKnowledgeAccessPolicy.class);

                /*
                 * 原有 ParentRetrievalOperatorTest
                 * 测试的重点不是 Tenant Policy。
                 *
                 * 默认让旧测试中的 Parent 作为 SHARED 可访问，
                 * 防止历史测试被 Step 5 无意义打碎。
                 */
                when(
                                tenantKnowledgeAccessPolicy
                                                .canAccessSharedOnly(
                                                                org.mockito.ArgumentMatchers.any(
                                                                                Document.class)))
                                .thenReturn(
                                                true);

                when(
                                tenantKnowledgeAccessPolicy
                                                .canAccess(
                                                                org.mockito.ArgumentMatchers.any(
                                                                                Document.class),
                                                                org.mockito.ArgumentMatchers.anyString()))
                                .thenReturn(
                                                true);

                operator = new ParentRetrievalOperator(
                                parentDocumentProvider,
                                tenantKnowledgeAccessPolicy);
        }

        @Test
        void shouldReplaceChunksWithParentDocuments() {
                Document chunkOne = createChildDocument(
                                "chunk-1",
                                "第四十六条部分内容",
                                "parent-1");

                Document chunkTwo = createChildDocument(
                                "chunk-2",
                                "第四十七条部分内容",
                                "parent-2");

                RetrieverContext context = createContext(
                                List.of(
                                                chunkOne,
                                                chunkTwo));

                Document parentOne = createDocument(
                                "parent-1",
                                "第一页完整 Parent 内容");

                Document parentTwo = createDocument(
                                "parent-2",
                                "第二页完整 Parent 内容");

                when(
                                parentDocumentProvider.findAllByIds(
                                                org.mockito.ArgumentMatchers.anyCollection()))
                                .thenReturn(
                                                List.of(parentOne, parentTwo));

                RetrieverContext result = operator.retrieve(context);

                assertNotSame(
                                context,
                                result);

                assertEquals(
                                List.of(
                                                parentOne,
                                                parentTwo),
                                result.getDocuments());

                /*
                 * 原始 Context 不被修改。
                 */
                assertEquals(
                                List.of(
                                                chunkOne,
                                                chunkTwo),
                                context.getDocuments());

                /*
                 * QueryContext 得到保留。
                 */
                assertSame(
                                context.getQueryContext(),
                                result.getQueryContext());
        }

        @Test
        void shouldDeduplicateParentIdsAndPreserveOrder() {
                Document firstChunk = createChildDocument(
                                "chunk-1",
                                "P2 的第一个 Chunk",
                                "parent-2");

                Document secondChunk = createChildDocument(
                                "chunk-2",
                                "P1 的第一个 Chunk",
                                "parent-1");

                Document thirdChunk = createChildDocument(
                                "chunk-3",
                                "P2 的第二个 Chunk",
                                "parent-2");

                RetrieverContext context = createContext(
                                List.of(
                                                firstChunk,
                                                secondChunk,
                                                thirdChunk));

                Document parentTwo = createDocument(
                                "parent-2",
                                "Parent 2");

                Document parentOne = createDocument(
                                "parent-1",
                                "Parent 1");

                when(
                                parentDocumentProvider.findAllByIds(
                                                org.mockito.ArgumentMatchers
                                                                .anyCollection()))
                                .thenReturn(
                                                List.of(
                                                                parentTwo,
                                                                parentOne));

                operator.retrieve(context);

                @SuppressWarnings("unchecked")
                ArgumentCaptor<Collection<String>> idsCaptor = ArgumentCaptor.forClass(
                                Collection.class);

                verify(
                                parentDocumentProvider).findAllByIds(
                                                idsCaptor.capture());

                assertEquals(
                                List.of(
                                                "parent-2",
                                                "parent-1"),
                                List.copyOf(
                                                idsCaptor.getValue()));
        }

        @Test
        void shouldReturnOriginalContextWhenDocumentsAreEmpty() {
                RetrieverContext context = RetrieverContext.from(
                                QueryContext.from(
                                                "劳动合同问题"));

                RetrieverContext result = operator.retrieve(context);

                assertSame(context, result);

                verify(
                                parentDocumentProvider,
                                never()).findAllByIds(
                                                org.mockito.ArgumentMatchers
                                                                .anyCollection());
        }

        @Test
        void shouldReturnOriginalContextWhenParentMetadataIsMissing() {
                Document chunk = createDocument(
                                "chunk-1",
                                "没有 Parent Metadata 的 Chunk");

                RetrieverContext context = createContext(
                                List.of(chunk));

                RetrieverContext result = operator.retrieve(context);

                assertSame(context, result);

                verify(
                                parentDocumentProvider,
                                never()).findAllByIds(
                                                org.mockito.ArgumentMatchers
                                                                .anyCollection());
        }

        @Test
        void shouldIgnoreBlankAndMissingParentIds() {
                List<Document> documents = new ArrayList<>();

                documents.add(
                                createChildDocument(
                                                "chunk-1",
                                                "有效 Chunk",
                                                "parent-1"));

                documents.add(
                                new Document(
                                                "chunk-2",
                                                "空 Parent ID",
                                                Map.of(
                                                                "parent_document_id",
                                                                "   ")));

                documents.add(
                                new Document(
                                                "chunk-3",
                                                "No Parent Metadata",
                                                Map.of()));

                RetrieverContext context = createContext(documents);

                Document parent = createDocument(
                                "parent-1",
                                "Parent 1");

                when(
                                parentDocumentProvider.findAllByIds(
                                                org.mockito.ArgumentMatchers
                                                                .anyCollection()))
                                .thenReturn(
                                                List.of(parent));

                RetrieverContext result = operator.retrieve(context);

                assertNotSame(context, result);

                assertEquals(
                                List.of(parent),
                                result.getDocuments());

                @SuppressWarnings("unchecked")
                ArgumentCaptor<Collection<String>> idsCaptor = ArgumentCaptor.forClass(
                                Collection.class);

                verify(
                                parentDocumentProvider)
                                .findAllByIds(
                                                idsCaptor.capture());

                assertEquals(
                                List.of("parent-1"),
                                List.copyOf(
                                                idsCaptor.getValue()));
        }

        @Test
        void shouldReturnOriginalContextWhenProviderReturnsEmpty() {
                Document chunk = createChildDocument(
                                "chunk-1",
                                "Chunk 内容",
                                "parent-1");

                RetrieverContext context = createContext(
                                List.of(chunk));

                when(
                                parentDocumentProvider.findAllByIds(
                                                List.of("parent-1")))
                                .thenReturn(
                                                List.of());

                RetrieverContext result = operator.retrieve(context);

                assertSame(context, result);

                assertEquals(
                                List.of(chunk),
                                result.getDocuments());
        }

        @Test
        void shouldReturnOriginalContextWhenProviderReturnsNull() {
                Document chunk = createChildDocument(
                                "chunk-1",
                                "Chunk 内容",
                                "parent-1");

                RetrieverContext context = createContext(
                                List.of(chunk));

                when(
                                parentDocumentProvider.findAllByIds(
                                                List.of("parent-1")))
                                .thenReturn(null);

                RetrieverContext result = operator.retrieve(context);

                assertSame(context, result);
        }

        @Test
        void shouldSupportNonStringParentIdMetadata() {
                Document chunk = new Document(
                                "chunk-1",
                                "Chunk 内容",
                                Map.of(
                                                "parent_document_id",
                                                10001));

                RetrieverContext context = createContext(
                                List.of(chunk));

                Document parent = createDocument(
                                "10001",
                                "Parent 内容");

                when(
                                parentDocumentProvider.findAllByIds(
                                                org.mockito.ArgumentMatchers
                                                                .anyCollection()))
                                .thenReturn(List.of(parent));

                RetrieverContext result = operator.retrieve(context);

                assertEquals(
                                List.of(parent),
                                result.getDocuments());
        }

        @Test
        void shouldThrowExceptionWhenContextIsNull() {
                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> operator.retrieve(null));

                assertEquals(
                                "RetrieverContext must not be null",
                                exception.getMessage());

                verify(
                                parentDocumentProvider,
                                never()).findAllByIds(
                                                org.mockito.ArgumentMatchers
                                                                .anyCollection());
        }

        @Test
        void shouldRejectNullProvider() {
                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> new ParentRetrievalOperator(
                                                null,
                                                tenantKnowledgeAccessPolicy));

                assertEquals(
                                "parentDocumentProvider must not be null",
                                exception.getMessage());
        }

        @Test
        void shouldRejectParentBelongingToAnotherTenant() {

                Document child = createChildDocument(
                                "chunk-1",
                                "Tenant A chunk",
                                "parent-1");

                RetrieverContext context = RetrieverContext.tenantAware(
                                QueryContext.from(
                                                "劳动合同问题"),
                                "tenant-a")
                                .toBuilder()
                                .documents(
                                                List.of(
                                                                child))
                                .build();

                Document unauthorizedParent = createDocument(
                                "parent-1",
                                "Tenant B parent");

                when(
                                parentDocumentProvider.findAllByIds(
                                                org.mockito.ArgumentMatchers
                                                                .anyCollection()))
                                .thenReturn(
                                                List.of(
                                                                unauthorizedParent));

                when(
                                tenantKnowledgeAccessPolicy.canAccess(
                                                unauthorizedParent,
                                                "tenant-a"))
                                .thenReturn(
                                                false);

                RetrieverContext result = operator.retrieve(
                                context);

                /*
                 * Parent 被拒绝后必须保留已经经过
                 * Vector Tenant Filter 的 Child，
                 * 不能使用 unauthorized Parent。
                 */
                assertEquals(
                                List.of(
                                                child),
                                result.getDocuments());
        }

        @Test
        void shouldAllowParentAccessibleToCurrentTenant() {

                Document child = createChildDocument(
                                "chunk-1",
                                "Tenant A chunk",
                                "parent-1");

                RetrieverContext context = RetrieverContext.tenantAware(
                                QueryContext.from(
                                                "劳动合同问题"),
                                "tenant-a")
                                .toBuilder()
                                .documents(
                                                List.of(
                                                                child))
                                .build();

                Document parent = createDocument(
                                "parent-1",
                                "Tenant A parent");

                when(
                                parentDocumentProvider.findAllByIds(
                                                org.mockito.ArgumentMatchers
                                                                .anyCollection()))
                                .thenReturn(
                                                List.of(
                                                                parent));

                when(
                                tenantKnowledgeAccessPolicy.canAccess(
                                                parent,
                                                "tenant-a"))
                                .thenReturn(
                                                true);

                RetrieverContext result = operator.retrieve(
                                context);

                assertEquals(
                                List.of(
                                                parent),
                                result.getDocuments());
        }

        private RetrieverContext createContext(
                        List<Document> documents) {

                return RetrieverContext.builder()
                                .queryContext(
                                                QueryContext.from(
                                                                "劳动合同解除需要赔偿吗？"))
                                .documents(documents)
                                .build();
        }

        private Document createChildDocument(
                        String id,
                        String text,
                        String parentId) {

                return new Document(
                                id,
                                text,
                                Map.of(
                                                "parent_document_id",
                                                parentId));
        }

        private Document createDocument(
                        String id,
                        String text) {

                return new Document(
                                id,
                                text,
                                Map.of());
        }
}