package com.quince.lawyeraiassistant.workflow.executor;

import com.quince.lawyeraiassistant.workflow.model.WorkflowContext;
import com.quince.lawyeraiassistant.workflow.model.WorkflowDefinition;
import com.quince.lawyeraiassistant.workflow.model.WorkflowNodeStatus;
import com.quince.lawyeraiassistant.workflow.model.WorkflowStatus;
import com.quince.lawyeraiassistant.workflow.node.WorkflowNode;
import com.quince.lawyeraiassistant.workflow.transition.WorkflowTransition;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 默认 Workflow Executor。
 *
 * <p>
 * 当前支持：
 * </p>
 *
 * <ul>
 * <li>顺序节点执行</li>
 * <li>条件 Transition</li>
 * <li>节点失败终止 Workflow</li>
 * <li>无匹配 Transition 时 Workflow 失败</li>
 * <li>最大执行步数 Guardrail</li>
 * </ul>
 */
public final class DefaultWorkflowExecutor
        implements WorkflowExecutor {

    /**
     * 防止错误的 Workflow Definition
     * 导致 Runtime 无限循环。
     */
    private static final int DEFAULT_MAX_STEPS = 100;

    private final List<WorkflowNodeExecutor> nodeExecutors;

    public DefaultWorkflowExecutor(
            List<WorkflowNodeExecutor> nodeExecutors) {

        this.nodeExecutors = List.copyOf(
                Objects.requireNonNull(
                        nodeExecutors,
                        "WorkflowNodeExecutors must not be null"));
    }

    @Override
    public WorkflowContext execute(
            WorkflowDefinition definition,
            WorkflowContext context) {

        Objects.requireNonNull(
                definition,
                "WorkflowDefinition must not be null");

        Objects.requireNonNull(
                context,
                "WorkflowContext must not be null");

        validateContext(
                definition,
                context);

        WorkflowContext current = context.withStatus(
                WorkflowStatus.RUNNING);

        int steps = 0;

        while (true) {

            /*
             * Runtime Guardrail。
             *
             * 防止：
             *
             * A -> B
             * ↑ ↓
             * └────┘
             *
             * 这样的 Workflow Definition
             * 导致无限执行。
             */
            if (++steps > DEFAULT_MAX_STEPS) {

                throw new IllegalStateException(
                        "Workflow maximum execution steps exceeded");
            }

            /*
             * 1. 获取当前 Node。
             */
            WorkflowNode node = definition.findNode(
                    current.getCurrentNodeId());

            /*
             * 2. Node:
             *
             * PENDING -> RUNNING
             */
            current = current.withNodeStatus(
                    node.getId(),
                    WorkflowNodeStatus.RUNNING);

            /*
             * 3. 找到负责当前 Node 的 Executor。
             */
            WorkflowNodeExecutor nodeExecutor = resolveExecutor(
                    node);

            /*
             * 4. 执行 Node。
             */
            WorkflowNodeExecutionResult result = nodeExecutor.execute(
                    node,
                    current);

            /*
             * 5. Node 执行失败。
             *
             * Node -> FAILED
             * Workflow -> FAILED
             *
             * 并记录 Failure Reason。
             */
            if (!result.isSuccess()) {

                return current
                        .withNodeStatus(
                                node.getId(),
                                WorkflowNodeStatus.FAILED)
                        .fail(
                                result.getErrorMessage());
            }

            /*
             * 6. Node 执行成功。
             *
             * 先合并 Result Variables，
             * 再把 Node 标记为 COMPLETED。
             *
             * 这里的顺序很重要：
             *
             * Condition Transition
             * 必须能够看到当前 Node
             * 最新产生的 Variables。
             */
            current = current
                    .mergeVariables(
                            result.getVariables())
                    .withNodeStatus(
                            node.getId(),
                            WorkflowNodeStatus.COMPLETED);

            /*
             * 7. 查找当前 Node 所有 outgoing transitions。
             */
            List<WorkflowTransition> outgoingTransitions = findOutgoingTransitions(
                    definition,
                    node.getId());

            if (outgoingTransitions.isEmpty()) {

                return current.withStatus(
                        WorkflowStatus.COMPLETED);
            }

            /*
             * current 在 Workflow Loop 中会持续重新赋值，
             * 因此不能直接被 Lambda 捕获。
             *
             * 创建当前时刻的不可变快照，
             * 专门用于 Condition 判断。
             */
            final WorkflowContext transitionContext = current;

            Optional<WorkflowTransition> matchedTransition = outgoingTransitions.stream()
                    .filter(
                            transition -> transition.matches(
                                    transitionContext))
                    .findFirst();

            if (matchedTransition.isEmpty()) {

                return current.fail(
                        "No matching transition found for node: "
                                + node.getId());
            }

            current = current.withCurrentNode(
                    matchedTransition
                            .orElseThrow()
                            .getToNodeId());
        }
    }

    /**
     * 校验 WorkflowContext 是否属于当前 WorkflowDefinition。
     */
    private void validateContext(
            WorkflowDefinition definition,
            WorkflowContext context) {

        if (!definition.getId()
                .equals(
                        context.getWorkflowId())) {

            throw new IllegalArgumentException(
                    "WorkflowContext does not belong to definition: "
                            + definition.getId());
        }
    }

    /**
     * 找到能够执行当前 WorkflowNode 的 Executor。
     */
    private WorkflowNodeExecutor resolveExecutor(
            WorkflowNode node) {

        return nodeExecutors.stream()
                .filter(
                        executor -> executor.supports(
                                node))
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException(
                                "No WorkflowNodeExecutor found for node: "
                                        + node.getId()));
    }

    /**
     * 获取当前 Node 的所有 outgoing transitions。
     *
     * 注意：
     *
     * 这里只负责查找 Transition，
     * 不负责 Condition 判断。
     */
    private List<WorkflowTransition> findOutgoingTransitions(
            WorkflowDefinition definition,
            String nodeId) {

        return definition.getTransitions()
                .stream()
                .filter(
                        transition -> transition.getFromNodeId()
                                .equals(
                                        nodeId))
                .toList();
    }
}