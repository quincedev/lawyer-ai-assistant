package com.quince.lawyeraiassistant.agent.action;

import com.quince.lawyeraiassistant.agent.model.AgentAction;
import com.quince.lawyeraiassistant.agent.model.AgentActionDecision;
import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentTask;
import com.quince.lawyeraiassistant.agent.model.RuntimeReasonObservation;
import com.quince.lawyeraiassistant.agent.model.ToolObservation;
import com.quince.lawyeraiassistant.agent.tool.AgentToolRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class SpringAiAgentActionSelector
                implements AgentActionSelector {

        private final ChatClient chatClient;

        private final AgentActionDecisionMapper decisionMapper;

        private final Resource actionSelectionPrompt;

        private final AgentToolRegistry toolRegistry;

        public SpringAiAgentActionSelector(
                        ChatClient.Builder chatClientBuilder,
                        AgentActionDecisionMapper decisionMapper,
                        AgentToolRegistry toolRegistry,
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

                this.actionSelectionPrompt = Objects.requireNonNull(
                                actionSelectionPrompt,
                                "actionSelectionPrompt must not be null");
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

                AgentActionDecision decision = chatClient.prompt()
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
                                                                                                context))
                                                                .param(
                                                                                "availableTools",
                                                                                resolveAvailableTools()))
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

                return context.getReasonResult()
                                .getReasonSummary();
        }

        private String resolveObservations(
                        AgentContext context) {

                String toolObservations = context.getObservations()
                                .stream()
                                .map(
                                                this::formatToolObservation)
                                .collect(
                                                Collectors.joining(
                                                                "\n\n"));

                String runtimeReasons = context.getRuntimeReasonObservations()
                                .stream()
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

        private String formatToolObservation(
                        ToolObservation observation) {

                if (observation.isFailure()) {

                        return """
                                        [TOOL]
                                        Task: %s
                                        Tool: %s
                                        Status: FAILED
                                        Error: %s
                                        """.formatted(
                                        observation.getTaskId(),
                                        observation.getToolName(),
                                        observation.getErrorMessage())
                                        .trim();
                }

                return """
                                [TOOL]
                                Task: %s
                                Tool: %s
                                Status: SUCCESS
                                Result:
                                %s
                                """.formatted(
                                observation.getTaskId(),
                                observation.getToolName(),
                                observation.getContent())
                                .trim();
        }

        private String formatRuntimeReasonObservation(
                        RuntimeReasonObservation observation) {

                return """
                                [REASON]
                                Task: %s
                                Result:
                                %s
                                """.formatted(
                                observation.getTaskId(),
                                observation.getContent())
                                .trim();
        }

        private String resolveAvailableTools() {

                if (toolRegistry.size() == 0) {
                        return "无";
                }

                return String.join(
                                "\n",
                                toolRegistry.names());
        }
}