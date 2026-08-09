package com.quince.lawyeraiassistant.prompt;

/**
 * Prompt 在 Registry 中使用的逻辑名称。
 *
 * <p>
 * 这里保存的是 Prompt 逻辑名称，而不是资源文件路径。
 * 业务代码通过逻辑名称获取 Prompt，不直接依赖 classpath 路径。
 * </p>
 *
 * <p>
 * 例如：
 * </p>
 *
 * <pre>
 * Prompt 名称：lawyer-system
 * 资源路径：classpath:prompts/system/lawyer-system.st
 * </pre>
 */
public final class PromptNames {

        /**
         * 律师助手基础身份和行为约束。
         */
        public static final String LAWYER_SYSTEM = "lawyer-system";

        /**
         * Agent Reason 阶段 Prompt。
         */
        public static final String AGENT_REASON = "agent-reason";

        /**
         * Agent Planning 阶段 Prompt。
         */
        public static final String AGENT_PLANNING = "agent-planning";

        /**
         * 法律知识引用规则。
         *
         * <p>
         * 当前只有项目中实际存在对应资源时才注册和使用。
         * </p>
         */
        public static final String CITATION_RULES = "citation-rules";

        /**
         * 无法可靠回答时的拒答规则。
         *
         * <p>
         * 当前只有项目中实际存在对应资源时才注册和使用。
         * </p>
         */
        public static final String REFUSE_RULES = "refuse-rules";

        /**
         * Agent Final Answer 阶段 Prompt。
         */
        public static final String AGENT_FINAL_ANSWER = "agent-final-answer";

        /**
         * Agent Reflection 阶段 Prompt。
         */
        public static final String AGENT_REFLECTION = "agent-reflection";

        /**
         * Agent Replanning 阶段 Prompt。
         */
        public static final String AGENT_REPLANNING = "agent-replanning";

        /**
         * Agent Runtime Reason 阶段 Prompt。
         */
        public static final String AGENT_RUNTIME_REASON = "agent-runtime-reason";

        private PromptNames() {
                throw new IllegalStateException(
                                "PromptNames is a constants class and cannot be instantiated");
        }
}