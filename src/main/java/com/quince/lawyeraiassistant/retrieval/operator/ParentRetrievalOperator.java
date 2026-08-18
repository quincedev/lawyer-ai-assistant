package com.quince.lawyeraiassistant.retrieval.operator;

import com.quince.lawyeraiassistant.rag.vector.tenant.TenantKnowledgeAccessPolicy;
import com.quince.lawyeraiassistant.retrieval.model.RetrieverContext;
import com.quince.lawyeraiassistant.retrieval.parent.provider.ParentDocumentProvider;

import org.springframework.ai.document.Document;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
@Order(200)
public class ParentRetrievalOperator
        implements RetrievalOperator {

    private static final String PARENT_DOCUMENT_ID = "parent_document_id";

    private final ParentDocumentProvider parentDocumentProvider;

    private final TenantKnowledgeAccessPolicy tenantKnowledgeAccessPolicy;

    public ParentRetrievalOperator(
            ParentDocumentProvider parentDocumentProvider,
            TenantKnowledgeAccessPolicy tenantKnowledgeAccessPolicy) {

        this.parentDocumentProvider = Objects.requireNonNull(
                parentDocumentProvider,
                "parentDocumentProvider must not be null");

        this.tenantKnowledgeAccessPolicy = Objects.requireNonNull(
                tenantKnowledgeAccessPolicy,
                "tenantKnowledgeAccessPolicy must not be null");
    }

    @Override
    public RetrieverContext retrieve(
            RetrieverContext context) {

        Objects.requireNonNull(
                context,
                "RetrieverContext must not be null");

        if (!context.hasDocuments()) {
            return context;
        }

        Set<String> parentDocumentIds = extractParentDocumentIds(
                context.getDocuments());

        if (parentDocumentIds.isEmpty()) {
            return context;
        }

        Collection<Document> parentDocuments = parentDocumentProvider.findAllByIds(
                parentDocumentIds);

        if (parentDocuments == null
                || parentDocuments.isEmpty()) {

            return context;
        }

        List<Document> accessibleParents = parentDocuments.stream()
                .filter(
                        Objects::nonNull)
                .filter(
                        document -> canAccess(
                                context,
                                document))
                .toList();

        /*
         * 不能回退到未经 Tenant 验证的 Parent。
         *
         * 当前 context 中的 Child Chunk 已经经过
         * VectorSearchOperator 的 Tenant Filter，
         * 因此如果没有合法 Parent，保留 Child 是安全的。
         */
        if (accessibleParents.isEmpty()) {
            return context;
        }

        return context.toBuilder()
                .documents(
                        accessibleParents)
                .build();
    }

    private boolean canAccess(
            RetrieverContext context,
            Document document) {

        if (context.hasTenantId()) {

            return tenantKnowledgeAccessPolicy.canAccess(
                    document,
                    context.requireTenantId());
        }

        /*
         * Legacy / non-tenant retrieval:
         *
         * fail closed -> SHARED ONLY.
         */
        return tenantKnowledgeAccessPolicy
                .canAccessSharedOnly(
                        document);
    }

    private Set<String> extractParentDocumentIds(
            List<Document> documents) {

        Set<String> parentDocumentIds = new LinkedHashSet<>();

        for (Document document : documents) {

            if (document == null) {
                continue;
            }

            Object rawParentId = document.getMetadata()
                    .get(
                            PARENT_DOCUMENT_ID);

            if (rawParentId == null) {
                continue;
            }

            String parentId = rawParentId.toString()
                    .trim();

            if (!parentId.isEmpty()) {

                parentDocumentIds.add(
                        parentId);
            }
        }

        return parentDocumentIds;
    }
}