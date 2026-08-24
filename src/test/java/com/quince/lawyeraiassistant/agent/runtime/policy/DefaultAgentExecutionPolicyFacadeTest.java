package com.quince.lawyeraiassistant.agent.runtime.policy;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.quince.lawyeraiassistant.security.SecurityTest;
import com.quince.lawyeraiassistant.security.audit.SecurityAuditEvent;
import com.quince.lawyeraiassistant.security.audit.SecurityAuditEventType;
import com.quince.lawyeraiassistant.security.audit.SecurityAuditLogger;
import com.quince.lawyeraiassistant.security.runtime.AgentExecutionBudget;
import com.quince.lawyeraiassistant.security.runtime.AgentExecutionLimits;
import com.quince.lawyeraiassistant.security.runtime.RuntimeGuardrailOperation;
import com.quince.lawyeraiassistant.security.runtime.RuntimeGuardrailResult;
import com.quince.lawyeraiassistant.security.runtime.RuntimeGuardrailService;
import com.quince.lawyeraiassistant.security.runtime.resource.RuntimeResourceGuardrailService;
import com.quince.lawyeraiassistant.security.runtime.resource.RuntimeResourceResult;
import com.quince.lawyeraiassistant.security.runtime.resource.RuntimeResourceType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SecurityTest
class DefaultAgentExecutionPolicyFacadeTest {

    private AgentExecutionLimits executionLimits;

    private RuntimeGuardrailService runtimeGuardrailService;

    private RuntimeResourceGuardrailService runtimeResourceGuardrailService;

    private SecurityAuditLogger securityAuditLogger;

    private DefaultAgentExecutionPolicyFacade facade;

    @BeforeEach
    void setUp() {

        executionLimits = new AgentExecutionLimits(
                10,
                8,
                2,
                3,
                Duration.ofSeconds(
                        120),
                Duration.ofSeconds(
                        30),
                20_000,
                60_000);

        runtimeGuardrailService = mock(
                RuntimeGuardrailService.class);

        runtimeResourceGuardrailService = mock(
                RuntimeResourceGuardrailService.class);

        securityAuditLogger = mock(
                SecurityAuditLogger.class);

        facade = new DefaultAgentExecutionPolicyFacade(
                executionLimits,
                runtimeGuardrailService,
                runtimeResourceGuardrailService,
                securityAuditLogger);
    }

    @Test
    void shouldCreateNewBudgetForEachExecution() {

        AgentExecutionBudget first = facade.createBudget();
        AgentExecutionBudget second = facade.createBudget();

        assertNotSame(
                first,
                second);

        assertSame(
                executionLimits,
                first.limits());
    }

    @Test
    void shouldReturnAllowedRuntimeResultWithoutAudit() {

        AgentExecutionBudget budget = facade.createBudget();
        RuntimeGuardrailResult expected = RuntimeGuardrailResult.allow(
                "runtime-policy");

        when(
                runtimeGuardrailService.evaluate(
                        RuntimeGuardrailOperation.STEP,
                        budget))
                .thenReturn(
                        expected);

        RuntimeGuardrailResult actual = facade.evaluateRuntime(
                RuntimeGuardrailOperation.STEP,
                budget);

        assertSame(
                expected,
                actual);

        verifyNoInteractions(
                securityAuditLogger);
    }

    @Test
    void shouldAuditDeniedRuntimeResult() {

        AgentExecutionBudget budget = facade.createBudget();
        RuntimeGuardrailResult expected = RuntimeGuardrailResult.deny(
                "step-limit",
                "Step limit reached");

        when(
                runtimeGuardrailService.evaluate(
                        RuntimeGuardrailOperation.STEP,
                        budget))
                .thenReturn(
                        expected);

        RuntimeGuardrailResult actual = facade.evaluateRuntime(
                RuntimeGuardrailOperation.STEP,
                budget);

        assertSame(
                expected,
                actual);

        ArgumentCaptor<SecurityAuditEvent> eventCaptor = ArgumentCaptor.forClass(
                SecurityAuditEvent.class);

        verify(securityAuditLogger)
                .log(
                        eventCaptor.capture());

        assertEquals(
                SecurityAuditEventType.RUNTIME_LIMIT_REACHED,
                eventCaptor.getValue().type());
    }

    @Test
    void shouldReturnAllowedResourceResultWithoutAudit() {

        RuntimeResourceResult expected = RuntimeResourceResult.allow(
                "observation-length");

        when(
                runtimeResourceGuardrailService.evaluate(
                        RuntimeResourceType.OBSERVATION,
                        1_000))
                .thenReturn(
                        expected);

        RuntimeResourceResult actual = facade.evaluateResource(
                RuntimeResourceType.OBSERVATION,
                1_000,
                "task-123");

        assertSame(
                expected,
                actual);

        verifyNoInteractions(
                securityAuditLogger);
    }

    @Test
    void shouldAuditDeniedResourceResultWithTaskId() {

        RuntimeResourceResult expected = RuntimeResourceResult.deny(
                "context-length",
                "Context limit reached");

        when(
                runtimeResourceGuardrailService.evaluate(
                        RuntimeResourceType.CONTEXT,
                        70_000))
                .thenReturn(
                        expected);

        RuntimeResourceResult actual = facade.evaluateResource(
                RuntimeResourceType.CONTEXT,
                70_000,
                "task-123");

        assertSame(
                expected,
                actual);

        ArgumentCaptor<SecurityAuditEvent> eventCaptor = ArgumentCaptor.forClass(
                SecurityAuditEvent.class);

        verify(securityAuditLogger)
                .log(
                        eventCaptor.capture());

        SecurityAuditEvent event = eventCaptor.getValue();

        assertEquals(
                SecurityAuditEventType.RESOURCE_LIMIT_REACHED,
                event.type());

        assertEquals(
                "task-123",
                event.metadata().get(
                        "taskId"));
    }
}
