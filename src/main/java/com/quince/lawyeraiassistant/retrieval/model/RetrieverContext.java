package com.quince.lawyeraiassistant.retrieval.model;

import com.quince.lawyeraiassistant.query.model.QueryContext;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Objects;

/**
 * Retriever Pipeline 的上下文对象。
 *
 * <p>
 * 负责保存一次检索请求在 Retriever Pipeline 中流转的数据。
 * </p>
 *
 * <p>
 * 当前第一版包含：
 * </p>
 *
 * <ul>
 * <li>queryContext：Query Pipeline 处理后的查询上下文</li>
 * <li>documents：当前阶段检索到的文档</li>
 * </ul>
 *
 * <p>
 * 本对象采用不可变设计。Retriever 节点不直接修改当前实例，
 * 而是通过 {@link #toBuilder()} 创建新的 RetrieverContext。
 * </p>
 */
@Getter
@ToString
@EqualsAndHashCode
public final class RetrieverContext {

    /**
     * Query Pipeline 产生的查询上下文。
     *
     * <p>
     * Retriever 应通过 QueryContext.effectiveQuery()
     * 获取当前真正用于检索的 Query。
     * </p>
     */
    private final QueryContext queryContext;

    /**
     * 当前 Retriever Pipeline 阶段产生的文档。
     *
     * <p>
     * 没有文档时统一使用空集合，不使用 null。
     * </p>
     */
    private final List<Document> documents;

    private final String tenantId;

    @Builder(toBuilder = true)
    private RetrieverContext(
            QueryContext queryContext,
            List<Document> documents,
            String tenantId) {
        this.queryContext = Objects.requireNonNull(
                queryContext,
                "QueryContext must not be null");

        this.documents = normalizeDocuments(documents);

        this.tenantId = normalizeOptionalTenantId(
                tenantId);
    }

    /**
     * 根据 QueryContext 创建初始 RetrieverContext。
     *
     * <p>
     * 初始状态下还没有执行检索，所以 documents 为空集合。
     * </p>
     *
     * @param queryContext Query Pipeline 输出的上下文
     * @return 初始 RetrieverContext
     */
    public static RetrieverContext from(
            QueryContext queryContext) {
        return RetrieverContext.builder()
                .queryContext(queryContext)
                .build();
    }

    /**
     * 获取当前真正应该用于检索的 Query。
     *
     * <p>
     * 如果 QueryContext 已产生 rewriteQuery，则使用改写结果；
     * 否则回退到原始 question。
     * </p>
     *
     * @return 当前有效检索 Query
     */
    public String effectiveQuery() {
        return queryContext.effectiveQuery();
    }

    /**
     * 判断当前上下文是否包含检索文档。
     */
    public boolean hasDocuments() {
        return !documents.isEmpty();
    }

    /**
     * 返回当前文档数量。
     */
    public int documentCount() {
        return documents.size();
    }

    public static RetrieverContext tenantAware(
            QueryContext queryContext,
            String tenantId) {

        Objects.requireNonNull(
                tenantId,
                "tenantId must not be null");

        String normalizedTenantId = tenantId.trim();

        if (normalizedTenantId.isEmpty()) {

            throw new IllegalArgumentException(
                    "tenantId must not be blank");
        }

        return RetrieverContext.builder()
                .queryContext(
                        queryContext)
                .tenantId(
                        normalizedTenantId)
                .build();
    }

    public boolean hasTenantId() {

        return tenantId != null;
    }

    public String requireTenantId() {

        if (tenantId == null) {

            throw new IllegalStateException(
                    "Tenant-aware retrieval requires tenantId");
        }

        return tenantId;
    }

    /**
     * 规范化文档列表。
     *
     * <p>
     * null 被转换为空集合，并通过 List.copyOf 创建不可修改快照。
     * List.copyOf 同时会拒绝列表中的 null 元素。
     * </p>
     */
    private static List<Document> normalizeDocuments(
            List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        return List.copyOf(documents);
    }

    private static String normalizeOptionalTenantId(
            String tenantId) {

        if (tenantId == null) {
            return null;
        }

        String normalized = tenantId.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}