package com.quince.lawyeraiassistant.agent.runtime.resource;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentTask;
import com.quince.lawyeraiassistant.agent.model.RuntimeReasonObservation;
import com.quince.lawyeraiassistant.agent.model.ToolObservation;
import com.quince.lawyeraiassistant.agent.skill.context.SkillContext;

@Component
public final class DefaultAgentContextSizeEstimator
        implements AgentContextSizeEstimator {

    @Override
    public int estimate(
            AgentContext context) {

        Objects.requireNonNull(
                context,
                "AgentContext must not be null");

        long length = 0L;

        length += lengthOf(
                context.getGoal());

        if (context.getReasonResult() != null) {

            length += lengthOf(
                    context.getReasonResult()
                            .getReasonSummary());
        }

        for (AgentTask task : context.getAgentPlan()
                .getTasks()) {

            length += lengthOf(
                    task.getId());

            length += lengthOf(
                    task.getDescription());

            if (task.getStatus() != null) {

                length += task.getStatus()
                        .name()
                        .length();
            }
        }

        for (ToolObservation observation : context.getObservations()) {

            length += lengthOf(
                    observation.getTaskId());

            length += lengthOf(
                    observation.getToolName());

            length += estimateObservation(
                    observation);
        }

        for (RuntimeReasonObservation observation : context.getRuntimeReasonObservations()) {

            length += lengthOf(
                    observation.getTaskId());

            length += lengthOf(
                    observation.getContent());
        }

        for (String executionLog : context.getExecutionLogs()) {

            length += lengthOf(
                    executionLog);
        }

        length += lengthOf(
                context.getFinalAnswer());

        if (context.hasSkill()) {

            SkillContext skillContext = context.getSkillContext()
                    .orElseThrow();

            length += lengthOf(
                    skillContext.getSkillId());

            length += lengthOf(
                    skillContext.getSkillName());

            length += lengthOf(
                    skillContext.getDescription());

            length += lengthOf(
                    skillContext.getInstructions());

            for (String toolName : skillContext.getAllowedTools()) {

                length += lengthOf(
                        toolName);
            }
        }

        return length > Integer.MAX_VALUE
                ? Integer.MAX_VALUE
                : (int) length;
    }

    @Override
    public int estimateObservation(
            ToolObservation observation) {

        Objects.requireNonNull(
                observation,
                "ToolObservation must not be null");

        if (observation.isSuccess()) {

            return lengthOf(
                    observation.getContent());
        }

        return lengthOf(
                observation.getErrorMessage());
    }

    @Override
    public int estimateText(
            String value) {

        return lengthOf(
                value);
    }

    private int lengthOf(
            String value) {

        return value == null
                ? 0
                : value.length();
    }
}