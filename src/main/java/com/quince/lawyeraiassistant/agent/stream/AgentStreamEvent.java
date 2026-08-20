package com.quince.lawyeraiassistant.agent.stream;

import java.time.Instant;
import java.util.Map;

public record AgentStreamEvent(
        AgentStreamEventType type,
        String taskId,
        String message,
        Map<String, Object> data,
        Instant timestamp) {

    public static AgentStreamEvent of(
            AgentStreamEventType type,
            String message) {

        return new AgentStreamEvent(
                type,
                null,
                message,
                Map.of(),
                Instant.now());
    }

    public static AgentStreamEvent task(
            AgentStreamEventType type,
            String taskId,
            String message) {

        return new AgentStreamEvent(
                type,
                taskId,
                message,
                Map.of(),
                Instant.now());
    }
}