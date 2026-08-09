package com.quince.lawyeraiassistant.agent.tool;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Agent Tool 注册中心。
 *
 * <p>
 * 负责维护 Tool Name 与 AgentTool 实例之间的映射关系。
 * </p>
 *
 * <p>
 * Spring 会自动注入所有 AgentTool 实现：
 * </p>
 *
 * <pre>
 * List&lt;AgentTool&gt;
 *      ↓
 * AgentToolRegistry
 *      ↓
 * Map&lt;String, AgentTool&gt;
 * </pre>
 */
@Component
public class AgentToolRegistry {

        private final Map<String, AgentTool> tools;

        public AgentToolRegistry(
                        List<AgentTool> tools) {

                Objects.requireNonNull(
                                tools,
                                "Agent tools must not be null");

                this.tools = buildToolMap(
                                tools);
        }

        /**
         * 根据 Tool Name 获取对应 Tool。
         *
         * @param toolName Tool 名称
         * @return AgentTool
         */
        public AgentTool get(
                        String toolName) {

                Objects.requireNonNull(
                                toolName,
                                "Tool name must not be null");

                String normalizedToolName = toolName.trim();

                if (normalizedToolName.isEmpty()) {
                        throw new IllegalArgumentException(
                                        "Tool name must not be blank");
                }

                AgentTool tool = tools.get(
                                normalizedToolName);

                if (tool == null) {
                        throw new IllegalArgumentException(
                                        "Agent tool not found: "
                                                        + normalizedToolName);
                }

                return tool;
        }

        /**
         * 判断 Tool 是否已经注册。
         */
        public boolean contains(
                        String toolName) {

                if (toolName == null) {
                        return false;
                }

                String normalizedToolName = toolName.trim();

                if (normalizedToolName.isEmpty()) {
                        return false;
                }

                return tools.containsKey(
                                normalizedToolName);
        }

        /**
         * 当前注册 Tool 数量。
         */
        public int size() {
                return tools.size();
        }

        /**
         * 返回当前注册的所有 Tool Name。
         */
        public List<String> names() {

                return List.copyOf(
                                tools.keySet());
        }

        private Map<String, AgentTool> buildToolMap(
                        List<AgentTool> agentTools) {

                Map<String, AgentTool> toolMap = new LinkedHashMap<>();

                for (AgentTool tool : agentTools) {

                        Objects.requireNonNull(
                                        tool,
                                        "Agent tool must not be null");

                        String toolName = normalizeToolName(
                                        tool.name());

                        AgentTool existingTool = toolMap.putIfAbsent(
                                        toolName,
                                        tool);

                        if (existingTool != null) {
                                throw new IllegalStateException(
                                                "Duplicate Agent tool name: "
                                                                + toolName);
                        }
                }

                return Map.copyOf(
                                toolMap);
        }

        private String normalizeToolName(
                        String toolName) {

                Objects.requireNonNull(
                                toolName,
                                "Agent tool name must not be null");

                String normalized = toolName.trim();

                if (normalized.isEmpty()) {
                        throw new IllegalArgumentException(
                                        "Agent tool name must not be blank");
                }

                return normalized;
        }
}