package com.quince.lawyeraiassistant.agent.prompt.builder;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentTask;
import com.quince.lawyeraiassistant.agent.model.RuntimeReasonObservation;
import com.quince.lawyeraiassistant.agent.prompt.model.FinalAnswerPromptContext;
import com.quince.lawyeraiassistant.security.legal.evidence.LegalEvidencePromptFormatter;

import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.stream.Collectors;

/**
 * FinalAnswerPromptContext Builder。
 *
 * <p>
 * 负责将 AgentContext 中 Final Answer 真正需要的信息
 * 转换为稳定的 Prompt 输入。
 * </p>
 */
@Component
public class FinalAnswerPromptContextBuilder {

        private final LegalEvidencePromptFormatter evidencePromptFormatter;

        public FinalAnswerPromptContextBuilder(
                        LegalEvidencePromptFormatter evidencePromptFormatter) {

                this.evidencePromptFormatter = Objects.requireNonNull(
                                evidencePromptFormatter,
                                "LegalEvidencePromptFormatter must not be null");
        }

        public FinalAnswerPromptContext build(
                        AgentContext context) {

                Objects.requireNonNull(
                                context,
                                "AgentContext must not be null");

                String reasonSummary = context.hasReasonResult()
                                ? context.getReasonResult()
                                                .getReasonSummary()
                                : null;

                String plan = formatPlan(
                                context);

                String observations = formatObservations(
                                context);

                return new FinalAnswerPromptContext(
                                context.getGoal(),
                                reasonSummary,
                                plan,
                                observations);
        }

        private String formatPlan(
                        AgentContext context) {

                if (context.getAgentPlan() == null
                                || context.getAgentPlan()
                                                .getTasks()
                                                .isEmpty()) {

                        return null;
                }

                return context.getAgentPlan()
                                .getTasks()
                                .stream()
                                .map(
                                                this::formatTask)
                                .collect(
                                                Collectors.joining(
                                                                "\n"));
        }

        private String formatTask(
                        AgentTask task) {

                return "%s | %s | %s".formatted(
                                task.getId(),
                                task.getStatus(),
                                task.getDescription());
        }

        private String formatObservations(
                        AgentContext context) {

                String toolObservations = context.getObservations()
                                .stream()
                                .map(
                                                evidencePromptFormatter::format)
                                .collect(
                                                Collectors.joining(
                                                                "\n\n---\n\n"));

                String runtimeReasonObservations = context.getRuntimeReasonObservations()
                                .stream()
                                .map(
                                                this::formatRuntimeReasonObservation)
                                .collect(
                                                Collectors.joining(
                                                                "\n\n---\n\n"));

                if (toolObservations.isBlank()
                                && runtimeReasonObservations.isBlank()) {

                        return null;
                }

                if (toolObservations.isBlank()) {
                        return runtimeReasonObservations;
                }

                if (runtimeReasonObservations.isBlank()) {
                        return toolObservations;
                }

                return toolObservations
                                + "\n\n---\n\n"
                                + runtimeReasonObservations;
        }

        private String formatRuntimeReasonObservation(
                        RuntimeReasonObservation observation) {

                return """
                                Type: REASON
                                Task: %s
                                Result:
                                %s
                                """.formatted(
                                observation.getTaskId(),
                                observation.getContent())
                                .trim();
        }

}