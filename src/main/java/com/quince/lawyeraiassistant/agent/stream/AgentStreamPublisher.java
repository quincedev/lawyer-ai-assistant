package com.quince.lawyeraiassistant.agent.stream;

public interface AgentStreamPublisher {

    void publish(
            AgentStreamEvent event);
}