package com.quince.lawyeraiassistant.agent.action.policy;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentTask;

@Component
public class EvidenceAwareActionPolicy {

        private static final List<String> ANALYTICAL_KEYWORDS = List.of(
                        "分析",
                        "归纳",
                        "总结",
                        "梳理",
                        "比较",
                        "区分",
                        "推理",
                        "形成结论",
                        "得出结论",
                        "综合");

        public boolean shouldPreferReason(
                        AgentContext context,
                        AgentTask currentTask) {

                Objects.requireNonNull(
                                context,
                                "AgentContext must not be null");

                Objects.requireNonNull(
                                currentTask,
                                "AgentTask must not be null");

                return isAnalyticalTask(currentTask)
                                && hasSuccessfulEvidence(context);
        }

        public boolean hasSuccessfulEvidence(
                        AgentContext context) {

                return context.getObservations()
                                .stream()
                                .anyMatch(
                                                observation -> !observation.isFailure());
        }

        private boolean isAnalyticalTask(
                        AgentTask task) {

                String description = resolveTaskDescription(task)
                                .toLowerCase(Locale.ROOT);

                return ANALYTICAL_KEYWORDS
                                .stream()
                                .anyMatch(
                                                description::contains);
        }

        private String resolveTaskDescription(
                        AgentTask task) {

                if (task.getDescription() == null) {

                        return "";
                }

                return task.getDescription()
                                .trim();
        }
}