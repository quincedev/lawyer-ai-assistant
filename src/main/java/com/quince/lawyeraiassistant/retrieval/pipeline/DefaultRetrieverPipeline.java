package com.quince.lawyeraiassistant.retrieval.pipeline;

import com.quince.lawyeraiassistant.rag.config.ConditionalOnRagEnabled;
import com.quince.lawyeraiassistant.retrieval.model.RetrieverContext;
import com.quince.lawyeraiassistant.retrieval.operator.RetrievalOperator;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * RetrieverPipeline 的默认实现。
 *
 * <p>
 * Spring 会自动注入所有 RetrievalOperator Bean。
 * Pipeline 根据 {@code @Order} 或 {@code Ordered}
 * 定义的顺序依次执行各个检索节点。
 * </p>
 *
 * <pre>
 * RetrieverContext
 *      ↓
 * VectorSearchOperator
 *      ↓
 * MergeOperator
 *      ↓
 * ParentRetrievalOperator
 *      ↓
 * ReRankOperator
 *      ↓
 * 最终 RetrieverContext
 * </pre>
 */
@Component
@ConditionalOnRagEnabled
public class DefaultRetrieverPipeline
        implements RetrieverPipeline {

    private final List<RetrievalOperator> operators;

    public DefaultRetrieverPipeline(
            List<RetrievalOperator> operators) {
        Objects.requireNonNull(
                operators,
                "RetrievalOperator list must not be null");

        List<RetrievalOperator> sortedOperators = new ArrayList<>(operators);

        validateOperators(sortedOperators);

        /*
         * 按照 @Order 或 Ordered 排序。
         *
         * 数值越小，越先执行。
         */
        AnnotationAwareOrderComparator.sort(
                sortedOperators);

        /*
         * 保存不可修改快照，防止外部代码在运行期间
         * 修改 Retriever Pipeline 的节点结构。
         */
        this.operators = List.copyOf(sortedOperators);
    }

    /**
     * 按顺序执行所有 RetrievalOperator。
     *
     * @param context 初始检索上下文
     * @return 最终检索上下文
     */
    @Override
    public RetrieverContext retrieve(
            RetrieverContext context) {
        Objects.requireNonNull(
                context,
                "RetrieverContext must not be null");

        RetrieverContext currentContext = context;

        for (RetrievalOperator operator : operators) {
            currentContext = operator.retrieve(
                    currentContext);

            if (currentContext == null) {
                throw new IllegalStateException(
                        "RetrievalOperator must not return null: "
                                + operator
                                        .getClass()
                                        .getName());
            }
        }

        return currentContext;
    }

    /**
     * 校验 Pipeline 节点列表。
     */
    private void validateOperators(
            List<RetrievalOperator> operators) {
        for (RetrievalOperator operator : operators) {
            if (operator == null) {
                throw new IllegalArgumentException(
                        "RetrievalOperator list must not contain null");
            }
        }
    }
}
