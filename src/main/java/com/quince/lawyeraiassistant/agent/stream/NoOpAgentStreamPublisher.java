package com.quince.lawyeraiassistant.agent.stream;

public final class NoOpAgentStreamPublisher
        implements AgentStreamPublisher {

    public static final NoOpAgentStreamPublisher INSTANCE = new NoOpAgentStreamPublisher();

    private NoOpAgentStreamPublisher() {
    }

    @Override
    public void publish(
            AgentStreamEvent event) {
        // no-op
    }
}