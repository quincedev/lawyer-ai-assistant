package com.quince.lawyeraiassistant.agent.tool;

import com.quince.lawyeraiassistant.agent.model.ToolAction;
import com.quince.lawyeraiassistant.agent.model.ToolExecutionResult;
import com.quince.lawyeraiassistant.agent.model.ToolObservation;
import com.quince.lawyeraiassistant.security.legal.LegalSecurityContext;
import com.quince.lawyeraiassistant.security.legal.SecuritySource;
import com.quince.lawyeraiassistant.security.legal.SecurityTrustLevel;
import com.quince.lawyeraiassistant.security.runtime.AgentExecutionLimits;
import com.quince.lawyeraiassistant.security.SecurityTest;
import com.quince.lawyeraiassistant.security.audit.SecurityAuditLogger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultToolActionExecutorTest {

        private AgentTool agentTool;

        private AgentToolRegistry toolRegistry;

        private DefaultToolActionExecutor executor;

        private AgentExecutionLimits executionLimits;

        private SecurityAuditLogger securityAuditLogger;

        @BeforeEach
        void setUp() {

                toolRegistry = mock(AgentToolRegistry.class);

                agentTool = mock(AgentTool.class);

                securityAuditLogger = mock(SecurityAuditLogger.class);

                executionLimits = new AgentExecutionLimits(
                                10,
                                8,
                                2,
                                3,
                                Duration.ofSeconds(120),
                                Duration.ofSeconds(30),
                                20_000,
                                60_000);

                when(
                                agentTool.resultSecuritySource())
                                .thenReturn(
                                                SecuritySource.TOOL_RESULT);

                when(
                                toolRegistry.get(
                                                "searchLegalKnowledge"))
                                .thenReturn(
                                                agentTool);

                executor = new DefaultToolActionExecutor(
                                toolRegistry,
                                executionLimits,
                                securityAuditLogger);
        }

        @AfterEach
        void tearDown() {

                executor.shutdown();
        }

        @Test
        void shouldExecuteToolSuccessfully() {

                ToolAction action = ToolAction.of(
                                "task-1",
                                "searchLegalKnowledge",
                                Map.of(
                                                "legalQuestion",
                                                "违法解除劳动合同"));

                when(
                                agentTool.execute(
                                                action))
                                .thenReturn(
                                                ToolExecutionResult.success(
                                                                "劳动合同法第八十七条规定..."));

                ToolObservation observation = executor.execute(
                                action);

                assertTrue(
                                observation.isSuccess());

                assertEquals(
                                "task-1",
                                observation.getTaskId());

                assertEquals(
                                "searchLegalKnowledge",
                                observation.getToolName());

                assertEquals(
                                "劳动合同法第八十七条规定...",
                                observation.getContent());

                LegalSecurityContext context = observation
                                .getEvidenceSecurityContext()
                                .orElseThrow();

                assertEquals(
                                SecuritySource.TOOL_RESULT,
                                context.source());

                assertEquals(
                                SecurityTrustLevel.UNTRUSTED,
                                context.trustLevel());

                verify(
                                agentTool)
                                .execute(
                                                action);
        }

        @Test
        void shouldConvertFailedResultToFailedObservation() {

                ToolAction action = ToolAction.of(
                                "task-1",
                                "searchLegalKnowledge");

                when(
                                agentTool.execute(
                                                action))
                                .thenReturn(
                                                ToolExecutionResult.failure(
                                                                "VectorStore unavailable"));

                ToolObservation observation = executor.execute(
                                action);

                assertTrue(
                                observation.isFailure());

                assertEquals(
                                "task-1",
                                observation.getTaskId());

                assertEquals(
                                "searchLegalKnowledge",
                                observation.getToolName());

                assertEquals(
                                "VectorStore unavailable",
                                observation.getErrorMessage());

                assertEquals(
                                SecuritySource.TOOL_RESULT,
                                observation
                                                .getEvidenceSecurityContext()
                                                .orElseThrow()
                                                .source());
        }

        @Test
        void shouldRejectNullAction() {

                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> executor.execute(
                                                null));

                assertEquals(
                                "ToolAction must not be null",
                                exception.getMessage());
        }

        @Test
        void shouldRejectNullToolRegistry() {

                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> new DefaultToolActionExecutor(
                                                null,
                                                executionLimits,
                                                securityAuditLogger));

                assertEquals(
                                "toolRegistry must not be null",
                                exception.getMessage());
        }

        @Test
        void shouldRejectNullToolExecutionResult() {

                ToolAction action = ToolAction.of(
                                "task-1",
                                "searchLegalKnowledge");

                when(
                                agentTool.execute(
                                                action))
                                .thenReturn(
                                                null);

                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> executor.execute(
                                                action));

                assertEquals(
                                "ToolExecutionResult must not be null",
                                exception.getMessage());
        }

        @SecurityTest
        @Test
        void shouldReturnFailureObservationWhenToolExecutionTimesOut() {

                AgentExecutionLimits shortLimits = new AgentExecutionLimits(
                                10,
                                8,
                                2,
                                3,
                                Duration.ofSeconds(120),
                                Duration.ofMillis(50),
                                20_000,
                                60_000);

                DefaultToolActionExecutor timeoutExecutor = new DefaultToolActionExecutor(
                                toolRegistry,
                                shortLimits,
                                securityAuditLogger);

                try {

                        ToolAction action = ToolAction.of(
                                        "task-1",
                                        "searchLegalKnowledge");

                        when(
                                        agentTool.execute(
                                                        action))
                                        .thenAnswer(
                                                        invocation -> {

                                                                Thread.sleep(
                                                                                500);

                                                                return ToolExecutionResult.success(
                                                                                "too late");
                                                        });

                        ToolObservation observation = timeoutExecutor.execute(
                                        action);

                        assertTrue(
                                        observation.isFailure());

                        assertEquals(
                                        "task-1",
                                        observation.getTaskId());

                        assertEquals(
                                        "searchLegalKnowledge",
                                        observation.getToolName());

                        assertEquals(
                                        "Tool execution timed out",
                                        observation.getErrorMessage());

                } finally {

                        timeoutExecutor.shutdown();
                }
        }

        @SecurityTest
        @Test
        void shouldConvertToolExceptionToFailureObservation() {

                ToolAction action = ToolAction.of(
                                "task-1",
                                "searchLegalKnowledge");

                when(
                                agentTool.execute(
                                                action))
                                .thenThrow(
                                                new IllegalStateException(
                                                                "Vector Store unavailable"));

                ToolObservation observation = executor.execute(
                                action);

                assertTrue(
                                observation.isFailure());

                assertEquals(
                                "Vector Store unavailable",
                                observation.getErrorMessage());
        }
}
