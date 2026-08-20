package com.quince.lawyeraiassistant.agent.runtime.metrics;

import java.util.Optional;

import org.springframework.stereotype.Component;

/**
 * 保存当前 Agent Execution 的性能指标。
 *
 * 注意：
 * Agent 当前存在 Virtual Thread / async Tool execution，
 * 因此后续不要依赖普通 ThreadLocal 做跨线程传播。
 */
@Component
public class AgentPerformanceContext {

    private final InheritableThreadLocal<AgentPerformanceMetrics> holder = new InheritableThreadLocal<>();

    public AgentPerformanceMetrics start() {

        AgentPerformanceMetrics metrics = new AgentPerformanceMetrics();

        holder.set(
                metrics);

        return metrics;
    }

    public Optional<AgentPerformanceMetrics> current() {

        return Optional.ofNullable(
                holder.get());
    }

    public void clear() {

        holder.remove();
    }
}