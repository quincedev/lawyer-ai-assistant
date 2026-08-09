package com.quince.lawyeraiassistant.agent.dto;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentStatus;

import java.util.List;
import java.util.Objects;

/**
 * Agent Playground 响应。
 *
 * @param goal          Agent 目标
 * @param reasonSummary Reason 阶段生成的目标理解摘要
 * @param plan          Planning 阶段生成的任务列表
 * @param observations  Tool 执行阶段产生的 Observation
 * @param finalAnswer   Agent 最终生成的用户答案
 * @param status        Agent 当前状态
 * @param executionLogs Agent 执行日志
 */
public record AgentResponse(

                String goal,

                String reasonSummary,

                List<AgentTaskResponse> plan,

                List<ToolObservationResponse> observations,

                String finalAnswer,

                AgentStatus status,

                List<String> executionLogs

) {

        public AgentResponse {

                Objects.requireNonNull(
                                goal,
                                "goal must not be null");

                Objects.requireNonNull(
                                status,
                                "status must not be null");

                plan = plan == null
                                ? List.of()
                                : List.copyOf(plan);

                observations = observations == null
                                ? List.of()
                                : List.copyOf(observations);

                executionLogs = executionLogs == null
                                ? List.of()
                                : List.copyOf(executionLogs);
        }

        public static AgentResponse from(
                        AgentContext context) {

                Objects.requireNonNull(
                                context,
                                "AgentContext must not be null");

                String reasonSummary = context.hasReasonResult()
                                ? context.getReasonResult()
                                                .getReasonSummary()
                                : null;

                List<AgentTaskResponse> plan = context.getAgentPlan()
                                .getTasks()
                                .stream()
                                .map(
                                                AgentTaskResponse::from)
                                .toList();

                List<ToolObservationResponse> observations = context.getObservations()
                                .stream()
                                .map(
                                                ToolObservationResponse::from)
                                .toList();

                return new AgentResponse(
                                context.getGoal(),
                                reasonSummary,
                                plan,
                                observations,
                                context.getFinalAnswer(),
                                context.getStatus(),
                                context.getExecutionLogs());
        }
}