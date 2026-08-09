package com.quince.lawyeraiassistant.agent.action;

import com.quince.lawyeraiassistant.agent.model.AgentAction;
import com.quince.lawyeraiassistant.agent.model.AgentActionDecision;
import com.quince.lawyeraiassistant.agent.model.AgentActionType;
import com.quince.lawyeraiassistant.agent.model.ToolAction;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

@Component
public class AgentActionDecisionMapper {

        public AgentAction map(
                        String taskId,
                        AgentActionDecision decision) {

                Objects.requireNonNull(
                                decision,
                                "AgentActionDecision must not be null");

                AgentActionType actionType = Objects.requireNonNull(
                                decision.actionType(),
                                "AgentActionType must not be null");

                return switch (actionType) {

                        case TOOL ->
                                mapToolAction(
                                                taskId,
                                                decision);

                        case REASON ->
                                AgentAction.reason(
                                                taskId);

                        case FINAL_ANSWER ->
                                AgentAction.finalAnswer(
                                                taskId);
                };
        }

        private AgentAction mapToolAction(
                        String taskId,
                        AgentActionDecision decision) {

                String toolName = normalizeToolName(
                                decision.toolName());

                Map<String, Object> arguments = decision.arguments() == null
                                ? Map.of()
                                : decision.arguments();

                ToolAction toolAction = ToolAction.of(
                                taskId,
                                toolName,
                                arguments);

                return AgentAction.tool(
                                toolAction);
        }

        private String normalizeToolName(
                        String toolName) {

                Objects.requireNonNull(
                                toolName,
                                "Tool name must not be null");

                String normalized = toolName.trim();

                if (normalized.isEmpty()) {
                        throw new IllegalArgumentException(
                                        "Tool name must not be blank");
                }

                return normalized;
        }
}