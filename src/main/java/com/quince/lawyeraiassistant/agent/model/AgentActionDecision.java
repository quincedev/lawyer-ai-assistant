package com.quince.lawyeraiassistant.agent.model;

import java.util.Map;

/**
 * LLM 对下一步 Agent Action 的结构化决策结果。
 *
 * <p>
 * 本对象属于 Action Decision 阶段的 Structured Output Model。
 * </p>
 *
 * <p>
 * 它不是最终 Runtime AgentAction。
 * Runtime 仍然需要对该结果进行校验和转换。
 * </p>
 *
 * <p>
 * 当前支持：
 * </p>
 *
 * <ul>
 * <li>TOOL</li>
 * <li>REASON</li>
 * <li>FINAL_ANSWER</li>
 * </ul>
 *
 * @param actionType Action 类型
 * @param toolName   Tool 名称，仅 TOOL Action 使用
 * @param arguments  Tool 参数，仅 TOOL Action 使用
 */
public record AgentActionDecision(

                AgentActionType actionType,

                String toolName,

                Map<String, Object> arguments

) {

        /**
         * 兼容 Day13 Tool-only Decision 的旧构造方式。
         *
         * <p>
         * 在 AgentActionSelector 和 Mapper
         * 正式完成 Multi-Action 升级前，
         * 旧代码仍然可以使用：
         * </p>
         *
         * <pre>
         * new AgentActionDecision(
         *                 "searchLegalKnowledge",
         *                 arguments)
         * </pre>
         *
         * <p>
         * 该构造方式默认映射为 TOOL Action。
         * </p>
         */
        public AgentActionDecision(
                        String toolName,
                        Map<String, Object> arguments) {

                this(
                                AgentActionType.TOOL,
                                toolName,
                                arguments);
        }
}