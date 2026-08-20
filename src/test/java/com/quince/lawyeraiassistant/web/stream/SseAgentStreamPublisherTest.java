package com.quince.lawyeraiassistant.web.stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.quince.lawyeraiassistant.agent.stream.AgentStreamEvent;
import com.quince.lawyeraiassistant.agent.stream.AgentStreamEventType;

class SseAgentStreamPublisherTest {

    @Test
    void shouldSendEventToEmitter() throws Exception {
        SseEmitter emitter = mock(SseEmitter.class);
        SseAgentStreamPublisher publisher = new SseAgentStreamPublisher(emitter);

        publisher.publish(
                AgentStreamEvent.of(
                        AgentStreamEventType.AGENT_STARTED,
                        "Agent execution started"));

        verify(emitter)
                .send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void shouldStopSendingAfterCompletion() throws Exception {
        SseEmitter emitter = mock(SseEmitter.class);
        ArgumentCaptor<Runnable> completionCaptor = ArgumentCaptor.forClass(Runnable.class);

        SseAgentStreamPublisher publisher = new SseAgentStreamPublisher(emitter);

        verify(emitter)
                .onCompletion(completionCaptor.capture());

        completionCaptor.getValue().run();

        publisher.publish(
                AgentStreamEvent.of(
                        AgentStreamEventType.AGENT_STARTED,
                        "ignored"));

        verify(emitter, times(0))
                .send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void shouldCloseAfterSendFailureAndIgnoreLaterEvents() throws Exception {
        SseEmitter emitter = mock(SseEmitter.class);

        doThrow(new IOException("client disconnected"))
                .when(emitter)
                .send(any(SseEmitter.SseEventBuilder.class));

        SseAgentStreamPublisher publisher = new SseAgentStreamPublisher(emitter);

        assertThrows(
                IllegalStateException.class,
                () -> publisher.publish(
                        AgentStreamEvent.of(
                                AgentStreamEventType.AGENT_STARTED,
                                "first")));

        publisher.publish(
                AgentStreamEvent.of(
                        AgentStreamEventType.AGENT_COMPLETED,
                        "second"));

        verify(emitter, times(1))
                .send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void shouldRejectNullEvent() {
        SseEmitter emitter = mock(SseEmitter.class);
        SseAgentStreamPublisher publisher = new SseAgentStreamPublisher(emitter);

        assertThrows(
                NullPointerException.class,
                () -> publisher.publish(null));
    }
}