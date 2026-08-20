package com.quince.lawyeraiassistant.agent.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentStatus;
import com.quince.lawyeraiassistant.agent.runtime.AgentRuntime;
import com.quince.lawyeraiassistant.agent.stream.AgentStreamEvent;
import com.quince.lawyeraiassistant.agent.stream.AgentStreamEventType;
import com.quince.lawyeraiassistant.agent.stream.AgentStreamPublisher;
import com.quince.lawyeraiassistant.security.audit.SecurityAuditLogger;
import com.quince.lawyeraiassistant.security.guardrail.GuardrailResult;
import com.quince.lawyeraiassistant.security.guardrail.exception.OutputGuardrailViolationException;
import com.quince.lawyeraiassistant.security.guardrail.input.InputGuardrailChain;
import com.quince.lawyeraiassistant.security.guardrail.output.OutputGuardrailChain;
import com.quince.lawyeraiassistant.security.identity.UserRole;
import com.quince.lawyeraiassistant.security.tenant.TenantContext;
import com.quince.lawyeraiassistant.security.tenant.TenantContextProvider;
import com.quince.lawyeraiassistant.security.tenant.authorization.TenantAuthorizationService;
import com.quince.lawyeraiassistant.security.tenant.quota.TenantQuotaLease;
import com.quince.lawyeraiassistant.security.tenant.quota.TenantResourceQuotaService;

class DefaultAgentApplicationServiceStreamingTest {

    private InputGuardrailChain inputGuardrailChain;
    private AgentRuntime agentRuntime;
    private OutputGuardrailChain outputGuardrailChain;
    private TenantContextProvider tenantContextProvider;
    private TenantAuthorizationService tenantAuthorizationService;
    private TenantResourceQuotaService tenantResourceQuotaService;
    private TenantQuotaLease tenantQuotaLease;
    private DefaultAgentApplicationService service;
    private TenantContext tenantContext;

    @BeforeEach
    void setUp() {
        inputGuardrailChain = mock(InputGuardrailChain.class);
        agentRuntime = mock(AgentRuntime.class);
        outputGuardrailChain = mock(OutputGuardrailChain.class);
        tenantContextProvider = mock(TenantContextProvider.class);
        tenantAuthorizationService = mock(TenantAuthorizationService.class);
        tenantResourceQuotaService = mock(TenantResourceQuotaService.class);
        tenantQuotaLease = mock(TenantQuotaLease.class);

        tenantContext = new TenantContext(
                "tenant-a",
                "user-001",
                "quince",
                Set.of(UserRole.LAWYER));

        when(tenantContextProvider.current())
                .thenReturn(tenantContext);

        when(tenantResourceQuotaService.acquireAgentExecution(tenantContext))
                .thenReturn(tenantQuotaLease);

        service = new DefaultAgentApplicationService(
                agentRuntime,
                inputGuardrailChain,
                outputGuardrailChain,
                mock(SecurityAuditLogger.class),
                tenantContextProvider,
                tenantAuthorizationService,
                tenantResourceQuotaService);
    }

    @Test
    void shouldUseStreamingRuntimeAndPublishAnswerOnlyAfterOutputGuardrailPasses() {
        String goal = "检索劳动合同解除需要满足哪些法律条件";
        String finalAnswer = "A".repeat(130) + "B".repeat(20);

        when(inputGuardrailChain.evaluate(goal))
                .thenReturn(GuardrailResult.allow("input"));

        AgentContext resultContext = AgentContext.authenticated(goal, tenantContext)
                .toBuilder()
                .status(AgentStatus.FINISHED)
                .finalAnswer(finalAnswer)
                .build();

        when(agentRuntime.run(any(AgentContext.class), any(AgentStreamPublisher.class)))
                .thenReturn(resultContext);

        when(outputGuardrailChain.evaluate(finalAnswer))
                .thenReturn(GuardrailResult.allow("output"));

        RecordingPublisher publisher = new RecordingPublisher();

        AgentContext result = service.executeStreaming(goal, publisher);

        assertSame(resultContext, result);

        verify(agentRuntime)
                .run(any(AgentContext.class), eq(publisher));

        verify(outputGuardrailChain)
                .evaluate(finalAnswer);

        assertEquals(
                List.of(
                        AgentStreamEventType.ANSWER_DELTA,
                        AgentStreamEventType.ANSWER_DELTA,
                        AgentStreamEventType.AGENT_COMPLETED),
                publisher.types());

        assertEquals(
                finalAnswer,
                publisher.answerText());
    }

    @Test
    void shouldNotPublishAnswerOrCompletedWhenOutputGuardrailBlocks() {
        String goal = "分析劳动合同";
        String finalAnswer = "blocked-output";

        when(inputGuardrailChain.evaluate(goal))
                .thenReturn(GuardrailResult.allow("input"));

        AgentContext resultContext = AgentContext.authenticated(goal, tenantContext)
                .toBuilder()
                .status(AgentStatus.FINISHED)
                .finalAnswer(finalAnswer)
                .build();

        when(agentRuntime.run(any(AgentContext.class), any(AgentStreamPublisher.class)))
                .thenReturn(resultContext);

        when(outputGuardrailChain.evaluate(finalAnswer))
                .thenReturn(GuardrailResult.block(
                        "output",
                        "sensitive output"));

        RecordingPublisher publisher = new RecordingPublisher();

        assertThrows(
                OutputGuardrailViolationException.class,
                () -> service.executeStreaming(goal, publisher));

        assertEquals(List.of(), publisher.types());
    }

    @Test
    void shouldRejectNullStreamingPublisherBeforeRuntime() {
        assertThrows(
                NullPointerException.class,
                () -> service.executeStreaming("分析劳动合同", null));

        verify(agentRuntime, never())
                .run(any(AgentContext.class), any(AgentStreamPublisher.class));
    }

    private static final class RecordingPublisher implements AgentStreamPublisher {

        private final List<AgentStreamEvent> events = new ArrayList<>();

        @Override
        public void publish(AgentStreamEvent event) {
            events.add(event);
        }

        private List<AgentStreamEventType> types() {
            return events.stream()
                    .map(AgentStreamEvent::type)
                    .toList();
        }

        private String answerText() {
            StringBuilder builder = new StringBuilder();

            events.stream()
                    .filter(event -> event.type() == AgentStreamEventType.ANSWER_DELTA)
                    .map(AgentStreamEvent::message)
                    .forEach(builder::append);

            return builder.toString();
        }
    }
}