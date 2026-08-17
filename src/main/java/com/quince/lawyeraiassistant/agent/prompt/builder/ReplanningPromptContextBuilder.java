package com.quince.lawyeraiassistant.agent.prompt.builder;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentTask;
import com.quince.lawyeraiassistant.agent.model.ReflectionResult;
import com.quince.lawyeraiassistant.agent.model.RuntimeReasonObservation;
import com.quince.lawyeraiassistant.agent.prompt.model.ReplanningPromptContext;
import com.quince.lawyeraiassistant.security.legal.evidence.LegalEvidencePromptFormatter;

import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Replanning Prompt Context Builder。
 */
@Component
public class ReplanningPromptContextBuilder {

        private final LegalEvidencePromptFormatter evidencePromptFormatter;

        public ReplanningPromptContextBuilder(
                        LegalEvidencePromptFormatter evidencePromptFormatter) {

                this.evidencePromptFormatter = Objects.requireNonNull(
                                evidencePromptFormatter,
                                "LegalEvidencePromptFormatter must not be null");
        }

        public ReplanningPromptContext build(
                        AgentContext context,
                        ReflectionResult reflectionResult) {

                Objects.requireNonNull(
                                context,
                                "AgentContext must not be null");

                Objects.requireNonNull(
                                reflectionResult,
                                "ReflectionResult must not be null");

                String reasonSummary = context.hasReasonResult()
                                ? context.getReasonResult()
                                                .getReasonSummary()
                                : null;

                String currentPlan = formatPlan(
                                context);

                String observations = formatObservations(
                                context);

                return new ReplanningPromptContext(
                                context.getGoal(),
                                reasonSummary,
                                currentPlan,
                                observations,
                                reflectionResult.getSummary());
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