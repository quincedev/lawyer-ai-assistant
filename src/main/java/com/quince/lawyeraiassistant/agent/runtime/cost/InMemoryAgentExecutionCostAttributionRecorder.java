package com.quince.lawyeraiassistant.agent.runtime.cost;

import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class InMemoryAgentExecutionCostAttributionRecorder
        implements AgentExecutionCostAttributionRecorder {

    private final AtomicReference<AgentExecutionCostAttribution> latest = new AtomicReference<>();

    @Override
    public void record(
            AgentExecutionCostAttribution attribution) {

        latest.set(
                Objects.requireNonNull(
                        attribution,
                        "attribution must not be null"));
    }

    @Override
    public Optional<AgentExecutionCostAttribution> latest() {

        return Optional.ofNullable(
                latest.get());
    }

    @Override
    public void clear() {

        latest.set(null);
    }
}