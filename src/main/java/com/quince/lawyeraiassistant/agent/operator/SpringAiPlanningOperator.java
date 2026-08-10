package com.quince.lawyeraiassistant.agent.operator;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentPlan;
import com.quince.lawyeraiassistant.agent.model.AgentStatus;
import com.quince.lawyeraiassistant.agent.prompt.model.PlanningPromptContext;
import com.quince.lawyeraiassistant.agent.service.AgentPlanningService;
import com.quince.lawyeraiassistant.agent.skill.context.SkillContext;
import com.quince.lawyeraiassistant.agent.skill.scope.SkillToolScope;
import com.quince.lawyeraiassistant.agent.tool.AgentToolRegistry;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * 基于 Spring AI 的 Planning Operator。
 *
 * <p>
 * 将 AgentContext 中的 Goal、ReasonResult、
 * Skill Instructions 和当前可用 Tool Scope
 * 转换为 PlanningPromptContext。
 * </p>
 */
@Component
@Order(300)
public class SpringAiPlanningOperator
                implements AgentOperator {

        private static final String PLANNING_COMPLETED_LOG = "Planning completed";

        private final AgentPlanningService agentPlanningService;

        private final SkillToolScope skillToolScope;

        private final AgentToolRegistry toolRegistry;

        public SpringAiPlanningOperator(
                        AgentPlanningService agentPlanningService,
                        SkillToolScope skillToolScope,
                        AgentToolRegistry toolRegistry) {

                this.agentPlanningService = Objects.requireNonNull(
                                agentPlanningService,
                                "agentPlanningService must not be null");

                this.skillToolScope = Objects.requireNonNull(
                                skillToolScope,
                                "skillToolScope must not be null");

                this.toolRegistry = Objects.requireNonNull(
                                toolRegistry,
                                "toolRegistry must not be null");
        }

        @Override
        public AgentContext execute(
                        AgentContext context) {

                Objects.requireNonNull(
                                context,
                                "AgentContext must not be null");

                if (!context.hasReasonResult()) {
                        throw new IllegalStateException(
                                        "ReasonResult must exist before planning");
                }

                PlanningPromptContext planningPromptContext = PlanningPromptContext.from(
                                context.getGoal(),
                                context.getReasonResult(),
                                resolveSkillInstructions(
                                                context),
                                resolveAvailableTools(
                                                context));

                AgentPlan agentPlan = agentPlanningService.plan(
                                planningPromptContext);

                Objects.requireNonNull(
                                agentPlan,
                                "AgentPlanningService must not return null");

                AgentContext plannedContext = context.toBuilder()
                                .agentPlan(agentPlan)
                                .status(
                                                AgentStatus.RUNNING)
                                .build();

                return plannedContext.appendExecutionLog(
                                PLANNING_COMPLETED_LOG);
        }

        private String resolveSkillInstructions(
                        AgentContext context) {

                return context.getSkillContext()
                                .map(
                                                SkillContext::getInstructions)
                                .orElse(
                                                "无");
        }

        private String resolveAvailableTools(
                        AgentContext context) {

                List<String> availableTools = skillToolScope.filterAllowed(
                                context.getSkillContext(),
                                toolRegistry.names());

                if (availableTools.isEmpty()) {
                        return "无";
                }

                return String.join(
                                ", ",
                                availableTools);
        }
}