package com.quince.lawyeraiassistant.agent.prompt.builder;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentTask;
import com.quince.lawyeraiassistant.agent.model.RuntimeReasonObservation;
import com.quince.lawyeraiassistant.agent.prompt.config.AgentPromptWindowProperties;
import com.quince.lawyeraiassistant.agent.prompt.model.ReflectionPromptContext;
import com.quince.lawyeraiassistant.security.legal.evidence.LegalEvidencePromptFormatter;

import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Reflection Prompt Context Builder。
 *
 * <p>
 * 负责将 AgentContext + 当前 AgentTask
 * 转换为 Reflection 阶段需要的 Prompt Context。
 * </p>
 */
@Component
public class ReflectionPromptContextBuilder {

        private final LegalEvidencePromptFormatter evidencePromptFormatter;

        private final AgentPromptWindowProperties promptWindowProperties;

        public ReflectionPromptContextBuilder(
                        LegalEvidencePromptFormatter evidencePromptFormatter,
                        AgentPromptWindowProperties promptWindowProperties) {

                this.evidencePromptFormatter = Objects.requireNonNull(
                                evidencePromptFormatter,
                                "LegalEvidencePromptFormatter must not be null");

                this.promptWindowProperties = Objects.requireNonNull(
                                promptWindowProperties,
                                "promptWindowProperties must not be null");
        }

        public ReflectionPromptContext build(
                        AgentContext context,
                        AgentTask task) {

                Objects.requireNonNull(
                                context,
                                "AgentContext must not be null");

                Objects.requireNonNull(
                                task,
                                "AgentTask must not be null");

                String reasonSummary = context.hasReasonResult()
                                ? context.getReasonResult()
                                                .getReasonSummary()
                                : null;

                String plan = formatPlan(
                                context);

                String observations = formatObservations(
                                context,
                                task);

                return new ReflectionPromptContext(
                                context.getGoal(),
                                reasonSummary,
                                task.getId(),
                                task.getDescription(),
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
                                                task -> "%s | %s | %s".formatted(
                                                                task.getId(),
                                                                task.getStatus(),
                                                                task.getDescription()))
                                .collect(
                                                Collectors.joining(
                                                                "\n"));
        }

        private String formatObservations(
                        AgentContext context,
                        AgentTask currentTask) {

                String toolObservations = context.getObservations()
                                .stream()
                                .filter(
                                                observation -> currentTask
                                                                .getId()
                                                                .equals(
                                                                                observation.getTaskId()))
                                .map(
                                                observation -> evidencePromptFormatter.format(
                                                                observation,
                                                                promptWindowProperties
                                                                                .getMaxEvidenceChars()))
                                .collect(
                                                Collectors.joining(
                                                                "\n\n---\n\n"));

                String runtimeReasonObservations = context.getRuntimeReasonObservations()
                                .stream()
                                .filter(
                                                observation -> currentTask
                                                                .getId()
                                                                .equals(
                                                                                observation.getTaskId()))
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