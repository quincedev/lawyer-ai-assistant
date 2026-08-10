package com.quince.lawyeraiassistant.agent.prompt.model;

import com.quince.lawyeraiassistant.agent.model.ReasonResult;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Map;
import java.util.Objects;

/**
 * Agent Planning Prompt 的动态上下文。
 *
 * <p>
 * Planning 阶段根据 Goal、ReasonResult、
 * Skill Instructions 和当前可用 Tools
 * 生成结构化 AgentPlan。
 * </p>
 */
@Getter
@ToString
@EqualsAndHashCode
public final class PlanningPromptContext {

        private final String goal;

        private final ReasonResult reasonResult;

        private final String skillInstructions;

        private final String availableTools;

        @Builder(toBuilder = true)
        private PlanningPromptContext(
                        String goal,
                        ReasonResult reasonResult,
                        String skillInstructions,
                        String availableTools) {

                this.goal = normalizeGoal(
                                goal);

                this.reasonResult = Objects.requireNonNull(
                                reasonResult,
                                "ReasonResult must not be null");

                this.skillInstructions = normalizeSkillInstructions(
                                skillInstructions);

                this.availableTools = normalizeAvailableTools(
                                availableTools);
        }

        public static PlanningPromptContext from(
                        String goal,
                        ReasonResult reasonResult,
                        String skillInstructions,
                        String availableTools) {

                return PlanningPromptContext.builder()
                                .goal(goal)
                                .reasonResult(reasonResult)
                                .skillInstructions(skillInstructions)
                                .availableTools(availableTools)
                                .build();
        }

        public Map<String, Object> toVariables() {

                return Map.of(
                                "goal",
                                goal,
                                "reasonSummary",
                                reasonResult.getReasonSummary(),
                                "skillInstructions",
                                skillInstructions,
                                "availableTools",
                                availableTools);
        }

        private static String normalizeGoal(
                        String goal) {

                Objects.requireNonNull(
                                goal,
                                "Goal must not be null");

                String normalizedGoal = goal.trim();

                if (normalizedGoal.isEmpty()) {
                        throw new IllegalArgumentException(
                                        "Goal must not be blank");
                }

                return normalizedGoal;
        }

        private static String normalizeSkillInstructions(
                        String skillInstructions) {

                if (skillInstructions == null
                                || skillInstructions.isBlank()) {

                        return "无";
                }

                return skillInstructions.trim();
        }

        private static String normalizeAvailableTools(
                        String availableTools) {

                if (availableTools == null
                                || availableTools.isBlank()) {

                        return "无";
                }

                return availableTools.trim();
        }
}