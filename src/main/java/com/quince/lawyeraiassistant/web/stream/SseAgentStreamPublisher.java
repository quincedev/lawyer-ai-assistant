package com.quince.lawyeraiassistant.web.stream;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.quince.lawyeraiassistant.agent.stream.AgentStreamEvent;
import com.quince.lawyeraiassistant.agent.stream.AgentStreamPublisher;

public class SseAgentStreamPublisher
        implements AgentStreamPublisher {

    private final SseEmitter emitter;

    private final AtomicBoolean closed = new AtomicBoolean(false);

    public SseAgentStreamPublisher(
            SseEmitter emitter) {

        this.emitter = Objects.requireNonNull(
                emitter,
                "SseEmitter must not be null");

        emitter.onCompletion(
                () -> closed.set(
                        true));

        emitter.onTimeout(
                () -> closed.set(
                        true));

        emitter.onError(
                error -> closed.set(
                        true));
    }

    @Override
    public void publish(
            AgentStreamEvent event) {

        Objects.requireNonNull(
                event,
                "AgentStreamEvent must not be null");

        if (closed.get()) {

            return;
        }

        try {

            emitter.send(
                    SseEmitter.event()
                            .name(
                                    event.type()
                                            .name()
                                            .toLowerCase())
                            .data(
                                    event));

        } catch (IOException exception) {

            closed.set(
                    true);

            throw new IllegalStateException(
                    "Failed to send SSE event",
                    exception);
        }
    }
}