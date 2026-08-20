package com.quince.lawyeraiassistant.agent.action.routing;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.quince.lawyeraiassistant.agent.model.AgentAction;
import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentTask;
import com.quince.lawyeraiassistant.agent.model.ToolAction;
import com.quince.lawyeraiassistant.agent.tool.legal.LegalToolContract;

@Component
public class DeterministicActionRouter {

    private static final String LEGAL_SEARCH_TOOL = "searchLegalKnowledge";

    private static final List<String> RETRIEVAL_KEYWORDS = List.of(
            "检索",
            "查询",
            "查找",
            "搜索",
            "核实",
            "验证",
            "获取法律依据",
            "获取外部依据");

    private static final List<String> ANALYTICAL_KEYWORDS = List.of(
            "分析",
            "归纳",
            "总结",
            "梳理",
            "比较",
            "区分",
            "推理",
            "形成结论",
            "风险提示",
            "构成要件",
            "法律后果");

    private static final List<String> REASONING_KEYWORDS = List.of(
            "识别",
            "判断",
            "理解",
            "分类",
            "框架",
            "核心争议点");

    public Optional<AgentAction> route(
            AgentContext context,
            AgentTask task) {

        Objects.requireNonNull(
                context,
                "AgentContext must not be null");

        Objects.requireNonNull(
                task,
                "AgentTask must not be null");

        /*
         * 1. 已有 Evidence 的分析型任务
         * → REASON
         */
        if (shouldReasonAfterSuccessfulEvidence(
                context,
                task)) {

            return Optional.of(
                    AgentAction.reason(
                            task.getId()));
        }

        /*
         * 2. 纯 Reasoning Task
         *
         * 即使描述中出现“检索方向”“检索范围”，
         * 只要任务本身是在识别/判断/规划检索方向，
         * 也不应该直接执行 Tool。
         */
        if (isPureReasoningTask(
                task)) {

            return Optional.of(
                    AgentAction.reason(
                            task.getId()));
        }

        /*
         * 3. 明确要求真正执行外部检索
         * → TOOL
         */
        if (isExplicitRetrievalTask(
                task)) {

            return Optional.of(
                    createLegalSearchAction(
                            task));
        }

        /*
         * 4. 当前 Task 已成功获取 Evidence
         * Reflection RETRY 后先 REASON
         */
        if (shouldReasonAfterRetry(
                context,
                task)) {

            return Optional.of(
                    AgentAction.reason(
                            task.getId()));
        }

        return Optional.empty();
    }

    private boolean shouldReasonAfterSuccessfulEvidence(
            AgentContext context,
            AgentTask task) {

        return isAnalyticalTask(task)
                && hasSuccessfulEvidence(context);
    }

    private boolean shouldReasonAfterRetry(
            AgentContext context,
            AgentTask task) {

        return hasSuccessfulEvidenceForTask(
                context,
                task);
    }

    private boolean hasSuccessfulEvidence(
            AgentContext context) {

        return context.getObservations()
                .stream()
                .anyMatch(
                        observation -> !observation.isFailure());
    }

    private boolean hasSuccessfulEvidenceForTask(
            AgentContext context,
            AgentTask task) {

        return context.getObservations()
                .stream()
                .filter(
                        observation -> task.getId()
                                .equals(
                                        observation.getTaskId()))
                .anyMatch(
                        observation -> !observation.isFailure());
    }

    private boolean isExplicitRetrievalTask(
            AgentTask task) {

        String description = normalize(
                task.getDescription());

        return RETRIEVAL_KEYWORDS
                .stream()
                .anyMatch(
                        description::contains);
    }

    private boolean isAnalyticalTask(
            AgentTask task) {

        String description = normalize(
                task.getDescription());

        return ANALYTICAL_KEYWORDS
                .stream()
                .anyMatch(
                        description::contains);
    }

    private String normalize(
            String value) {

        return value == null
                ? ""
                : value
                        .trim()
                        .toLowerCase(
                                Locale.ROOT);
    }

    private boolean isPureReasoningTask(
            AgentTask task) {

        String description = normalize(
                task.getDescription());

        boolean containsReasoningKeyword = REASONING_KEYWORDS
                .stream()
                .anyMatch(
                        description::contains);

        if (!containsReasoningKeyword) {

            return false;
        }

        /*
         * “确定检索范围 / 检索方向 / 检索策略”
         * 本质仍然是 Reasoning，
         * 不代表现在就执行 Tool。
         */
        return !containsExplicitToolExecutionIntent(
                description);
    }

    private boolean containsExplicitToolExecutionIntent(
            String description) {

        return description.contains(
                "调用searchlegalknowledge")
                || description.contains(
                        "使用searchlegalknowledge")
                || description.contains(
                        "执行检索")
                || description.contains(
                        "检索《")
                || description.contains(
                        "检索劳动合同")
                || description.startsWith(
                        "检索")
                || description.startsWith(
                        "查询")
                || description.startsWith(
                        "搜索");
    }

    private AgentAction createLegalSearchAction(
            AgentTask task) {

        String legalQuestion = task.getDescription();

        ToolAction toolAction = ToolAction.of(
                task.getId(),
                LEGAL_SEARCH_TOOL,
                java.util.Map.of(
                        LegalToolContract.LEGAL_QUESTION,
                        legalQuestion));

        return AgentAction.tool(
                toolAction);
    }
}