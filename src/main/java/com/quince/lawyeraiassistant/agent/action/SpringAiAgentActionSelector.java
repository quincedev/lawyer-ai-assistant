package com.quince.lawyeraiassistant.agent.action;

import com.quince.lawyeraiassistant.agent.action.policy.EvidenceAwareActionPolicy;
import com.quince.lawyeraiassistant.agent.model.AgentAction;
import com.quince.lawyeraiassistant.agent.model.AgentActionDecision;
import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentTask;
import com.quince.lawyeraiassistant.agent.model.RuntimeReasonObservation;
import com.quince.lawyeraiassistant.agent.model.ToolObservation;
import com.quince.lawyeraiassistant.agent.prompt.config.AgentPromptWindowProperties;
import com.quince.lawyeraiassistant.agent.skill.context.SkillContext;
import com.quince.lawyeraiassistant.agent.skill.scope.SkillToolScope;
import com.quince.lawyeraiassistant.agent.tool.AgentToolRegistry;
import com.quince.lawyeraiassistant.security.legal.evidence.LegalEvidencePromptFormatter;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class SpringAiAgentActionSelector
                implements AgentActionSelector {

        private final EvidenceAwareActionPolicy evidenceAwareActionPolicy;

        private final LegalEvidencePromptFormatter evidencePromptFormatter;

        private final ChatClient chatClient;

        private final AgentActionDecisionMapper decisionMapper;

        private final Resource actionSelectionPrompt;

        private final AgentToolRegistry toolRegistry;

        private final SkillToolScope skillToolScope;

        private final AgentPromptWindowProperties promptWindowProperties;

        public SpringAiAgentActionSelector(
                        ChatClient.Builder chatClientBuilder,
                        AgentActionDecisionMapper decisionMapper,
                        AgentToolRegistry toolRegistry,
                        SkillToolScope skillToolScope,
                        LegalEvidencePromptFormatter evidencePromptFormatter,
                        AgentPromptWindowProperties promptWindowProperties,
                        EvidenceAwareActionPolicy evidenceAwareActionPolicy,
                        @Value("classpath:/prompts/agent/action-selection.st") Resource actionSelectionPrompt) {

                this.chatClient = Objects.requireNonNull(
                                chatClientBuilder,
                                "chatClientBuilder must not be null")
                                .build();

                this.decisionMapper = Objects.requireNonNull(
                                decisionMapper,
                                "decisionMapper must not be null");

                this.toolRegistry = Objects.requireNonNull(
                                toolRegistry,
                                "toolRegistry must not be null");

                this.skillToolScope = Objects.requireNonNull(
                                skillToolScope,
                                "skillToolScope must not be null");

                this.actionSelectionPrompt = Objects.requireNonNull(
                                actionSelectionPrompt,
                                "actionSelectionPrompt must not be null");

                this.evidencePromptFormatter = Objects.requireNonNull(
                                evidencePromptFormatter,
                                "LegalEvidencePromptFormatter must not be null");

                this.promptWindowProperties = Objects.requireNonNull(
                                promptWindowProperties,
                                "promptWindowProperties must not be null");

                this.evidenceAwareActionPolicy = Objects.requireNonNull(
                                evidenceAwareActionPolicy,
                                "evidenceAwareActionPolicy must not be null");
        }

        @Override
        public AgentAction select(
                        AgentContext context,
                        AgentTask task) {

                Objects.requireNonNull(
                                context,
                                "AgentContext must not be null");

                Objects.requireNonNull(
                                task,
                                "AgentTask must not be null");

                String actionPolicyHint = resolveActionPolicyHint(
                                context,
                                task);

                AgentActionDecision decision = chatClient
                                .prompt()
                                .user(
                                                userSpec -> userSpec
                                                                .text(
                                                                                actionSelectionPrompt)
                                                                .param(
                                                                                "goal",
                                                                                context.getGoal())
                                                                .param(
                                                                                "reasonSummary",
                                                                                resolveReasonSummary(
                                                                                                context))
                                                                .param(
                                                                                "taskId",
                                                                                task.getId())
                                                                .param(
                                                                                "taskDescription",
                                                                                task.getDescription())
                                                                .param(
                                                                                "observations",
                                                                                resolveObservations(
                                                                                                context,
                                                                                                task))
                                                                .param(
                                                                                "availableTools",
                                                                                resolveAvailableTools(
                                                                                                context))
                                                                .param(
                                                                                "skillName",
                                                                                resolveSkillName(
                                                                                                context))
                                                                .param(
                                                                                "skillInstructions",
                                                                                resolveSkillInstructions(
                                                                                                context))
                                                                .param(
                                                                                "actionPolicyHint",
                                                                                actionPolicyHint))
                                .call()
                                .entity(
                                                AgentActionDecision.class);

                return decisionMapper.map(
                                task.getId(),
                                decision);
        }

        private String resolveReasonSummary(
                        AgentContext context) {

                if (!context.hasReasonResult()) {
                        return "无";
                }

                return context
                                .getReasonResult()
                                .getReasonSummary();
        }

        private String resolveObservations(
                        AgentContext context,
                        AgentTask currentTask) {

                List<ToolObservation> historical = latestHistoricalObservations(
                                context,
                                currentTask);

                List<ToolObservation> current = context.getObservations()
                                .stream()
                                .filter(
                                                observation -> currentTask
                                                                .getId()
                                                                .equals(
                                                                                observation.getTaskId()))
                                .toList();

                String toolObservations = java.util.stream.Stream.concat(
                                historical.stream(),
                                current.stream())
                                .map(
                                                observation -> evidencePromptFormatter.format(
                                                                observation,
                                                                promptWindowProperties
                                                                                .getMaxEvidenceChars()))
                                .collect(
                                                Collectors.joining(
                                                                "\n\n"));

                String runtimeReasons = context.getRuntimeReasonObservations()
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
                                                                "\n\n"));

                String combined = (toolObservations
                                + "\n\n"
                                + runtimeReasons)
                                .trim();

                return combined.isEmpty()
                                ? "无"
                                : combined;
        }

        private String resolveAvailableTools(
                        AgentContext context) {

                List<String> effectiveToolNames = skillToolScope.filterAllowed(
                                context.getSkillContext(),
                                toolRegistry.names());

                if (effectiveToolNames.isEmpty()) {
                        return "无";
                }

                return String.join(
                                "\n",
                                effectiveToolNames);
        }

        private String formatRuntimeReasonObservation(
                        RuntimeReasonObservation observation) {

                return """
                                [REASON]
                                Task: %s
                                Result:
                                %s
                                """
                                .formatted(
                                                observation.getTaskId(),
                                                observation.getContent())
                                .trim();
        }

        private String resolveSkillName(
                        AgentContext context) {

                return context.getSkillContext()
                                .map(
                                                SkillContext::getSkillName)
                                .orElse(
                                                "无");
        }

        private String resolveSkillInstructions(
                        AgentContext context) {

                return context.getSkillContext()
                                .map(
                                                SkillContext::getInstructions)
                                .orElse(
                                                "无");
        }

        private List<ToolObservation> latestHistoricalObservations(
                        AgentContext context,
                        AgentTask currentTask) {

                List<ToolObservation> historical = context.getObservations()
                                .stream()
                                .filter(
                                                observation -> !currentTask
                                                                .getId()
                                                                .equals(
                                                                                observation.getTaskId()))
                                .toList();

                int limit = promptWindowProperties
                                .getMaxHistoricalObservations();

                if (limit == 0
                                || historical.isEmpty()) {

                        return List.of();
                }

                int fromIndex = Math.max(
                                0,
                                historical.size() - limit);

                return historical.subList(
                                fromIndex,
                                historical.size());
        }

        private String resolveActionPolicyHint(
                        AgentContext context,
                        AgentTask task) {

                if (evidenceAwareActionPolicy
                                .shouldPreferReason(
                                                context,
                                                task)) {

                        return """
                                        当前 Task 属于分析、归纳、总结或推理类任务，
                                        且 AgentContext 中已经存在成功的外部 Evidence。

                                        本任务应优先使用 REASON,
                                        基于已有 Evidence 完成分析，
                                        不应为了重复获取相同资料再次调用 Tool。
                                        """
                                        .trim();
                }

                return """
                                根据当前 Task 的真实需要，
                                在 REASON 与可用 Tool 之间选择最合适的 Action。
                                """
                                .trim();
        }
}