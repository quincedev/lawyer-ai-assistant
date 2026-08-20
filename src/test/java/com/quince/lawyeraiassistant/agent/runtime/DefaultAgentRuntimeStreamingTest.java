package com.quince.lawyeraiassistant.agent.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.quince.lawyeraiassistant.agent.action.AgentActionSelector;
import com.quince.lawyeraiassistant.agent.action.policy.DuplicateToolCallPolicy;
import com.quince.lawyeraiassistant.agent.action.policy.NoProgressRetryPolicy;
import com.quince.lawyeraiassistant.agent.action.routing.DeterministicActionRouter;
import com.quince.lawyeraiassistant.agent.model.AgentAction;
import com.quince.lawyeraiassistant.agent.model.AgentActionExecutionResult;
import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentPlan;
import com.quince.lawyeraiassistant.agent.model.AgentStatus;
import com.quince.lawyeraiassistant.agent.model.AgentTask;
import com.quince.lawyeraiassistant.agent.model.ReflectionDecision;
import com.quince.lawyeraiassistant.agent.model.ReflectionResult;
import com.quince.lawyeraiassistant.agent.model.ToolAction;
import com.quince.lawyeraiassistant.agent.model.ToolObservation;
import com.quince.lawyeraiassistant.agent.operator.AgentActionExecutionOperator;
import com.quince.lawyeraiassistant.agent.pipeline.AgentPipeline;
import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceContext;
import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceSnapshotRecorder;
import com.quince.lawyeraiassistant.agent.runtime.metrics.micrometer.AgentMicrometerMetrics;
import com.quince.lawyeraiassistant.agent.service.AgentFinalAnswerService;
import com.quince.lawyeraiassistant.agent.service.AgentReflectionService;
import com.quince.lawyeraiassistant.agent.service.AgentReplanningService;
import com.quince.lawyeraiassistant.agent.skill.selector.AgentSkillSelector;
import com.quince.lawyeraiassistant.agent.stream.AgentStreamEvent;
import com.quince.lawyeraiassistant.agent.stream.AgentStreamEventType;
import com.quince.lawyeraiassistant.agent.stream.AgentStreamPublisher;
import com.quince.lawyeraiassistant.security.audit.SecurityAuditLogger;
import com.quince.lawyeraiassistant.security.legal.LegalSecurityContext;
import com.quince.lawyeraiassistant.security.legal.SecuritySource;
import com.quince.lawyeraiassistant.security.legal.SecurityTrustLevel;
import com.quince.lawyeraiassistant.security.legal.evidence.LegalEvidenceTrustPolicy;
import com.quince.lawyeraiassistant.security.runtime.AgentExecutionLimits;
import com.quince.lawyeraiassistant.security.runtime.DefaultRuntimeGuardrailService;
import com.quince.lawyeraiassistant.security.runtime.RuntimeGuardrailService;
import com.quince.lawyeraiassistant.security.runtime.policy.ExecutionTimeRuntimeGuardrailPolicy;
import com.quince.lawyeraiassistant.security.runtime.policy.ReplanLimitRuntimeGuardrailPolicy;
import com.quince.lawyeraiassistant.security.runtime.policy.RetryLimitRuntimeGuardrailPolicy;
import com.quince.lawyeraiassistant.security.runtime.policy.StepLimitRuntimeGuardrailPolicy;
import com.quince.lawyeraiassistant.security.runtime.policy.ToolCallLimitRuntimeGuardrailPolicy;
import com.quince.lawyeraiassistant.security.runtime.resource.DefaultRuntimeResourceGuardrailService;
import com.quince.lawyeraiassistant.security.runtime.resource.RuntimeResourceGuardrailService;
import com.quince.lawyeraiassistant.security.runtime.resource.policy.ContextLengthRuntimeResourcePolicy;
import com.quince.lawyeraiassistant.security.runtime.resource.policy.ObservationLengthRuntimeResourcePolicy;

class DefaultAgentRuntimeStreamingTest {

        private AgentPipeline agentPipeline;
        private AgentSkillSelector skillSelector;
        private AgentActionSelector actionSelector;
        private AgentActionExecutionOperator actionExecutionOperator;
        private AgentReflectionService reflectionService;
        private AgentReplanningService replanningService;
        private AgentFinalAnswerService finalAnswerService;
        private SecurityAuditLogger securityAuditLogger;
        private DeterministicActionRouter deterministicActionRouter;
        private DefaultAgentRuntime runtime;
        private AgentMicrometerMetrics micrometerMetrics;
        private AgentPerformanceSnapshotRecorder performanceSnapshotRecorder;

        @BeforeEach
        void setUp() {
                agentPipeline = mock(AgentPipeline.class);
                skillSelector = mock(AgentSkillSelector.class);
                actionSelector = mock(AgentActionSelector.class);
                actionExecutionOperator = mock(AgentActionExecutionOperator.class);
                reflectionService = mock(AgentReflectionService.class);
                replanningService = mock(AgentReplanningService.class);
                finalAnswerService = mock(AgentFinalAnswerService.class);
                securityAuditLogger = mock(SecurityAuditLogger.class);
                deterministicActionRouter = mock(DeterministicActionRouter.class);
                micrometerMetrics = mock(AgentMicrometerMetrics.class);
                performanceSnapshotRecorder = mock(AgentPerformanceSnapshotRecorder.class);

                AgentExecutionLimits limits = new AgentExecutionLimits(
                                10,
                                8,
                                2,
                                3,
                                Duration.ofSeconds(120),
                                Duration.ofSeconds(30),
                                20_000,
                                60_000);

                RuntimeGuardrailService runtimeGuardrailService = new DefaultRuntimeGuardrailService(
                                List.of(
                                                new ExecutionTimeRuntimeGuardrailPolicy(),
                                                new StepLimitRuntimeGuardrailPolicy(),
                                                new ToolCallLimitRuntimeGuardrailPolicy(),
                                                new ReplanLimitRuntimeGuardrailPolicy(),
                                                new RetryLimitRuntimeGuardrailPolicy()));

                RuntimeResourceGuardrailService resourceGuardrailService = new DefaultRuntimeResourceGuardrailService(
                                List.of(
                                                new ObservationLengthRuntimeResourcePolicy(),
                                                new ContextLengthRuntimeResourcePolicy()),
                                limits);

                runtime = new DefaultAgentRuntime(
                                agentPipeline,
                                skillSelector,
                                actionSelector,
                                actionExecutionOperator,
                                reflectionService,
                                replanningService,
                                finalAnswerService,
                                limits,
                                runtimeGuardrailService,
                                resourceGuardrailService,
                                new LegalEvidenceTrustPolicy(),
                                securityAuditLogger,
                                new DuplicateToolCallPolicy(),
                                deterministicActionRouter,
                                new NoProgressRetryPolicy(),
                                new AgentPerformanceContext(),
                                ignored -> List.of(),
                                micrometerMetrics,
                                performanceSnapshotRecorder);

                when(skillSelector.select(any()))
                                .thenReturn(Optional.empty());
        }

        @Test
        void shouldPublishLifecycleEventsForToolAndReasonTasks() {
                AgentTask toolTask = AgentTask.pending(
                                "task-1",
                                "调用searchLegalKnowledge检索劳动合同解除法律依据");

                AgentTask reasonTask = AgentTask.pending(
                                "task-2",
                                "基于检索结果分析法律条件");

                AgentContext initialized = AgentContext.builder()
                                .goal("检索劳动合同解除需要满足哪些法律条件")
                                .agentPlan(AgentPlan.from(List.of(toolTask, reasonTask)))
                                .status(AgentStatus.RUNNING)
                                .build();

                ToolAction toolAction = ToolAction.of(
                                "task-1",
                                "searchLegalKnowledge",
                                Map.of("legalQuestion", "劳动合同解除法律条件"));

                ToolObservation observation = ToolObservation.success(
                                "task-1",
                                "searchLegalKnowledge",
                                "《劳动合同法》相关法律依据",
                                LegalSecurityContext.of(
                                                SecuritySource.MCP_RESULT,
                                                SecurityTrustLevel.UNTRUSTED));

                when(agentPipeline.execute(any()))
                                .thenReturn(initialized);

                when(deterministicActionRouter.route(any(), any()))
                                .thenReturn(
                                                Optional.of(AgentAction.tool(toolAction)),
                                                Optional.of(AgentAction.reason("task-2")));

                when(actionExecutionOperator.execute(any(), any(), any()))
                                .thenReturn(
                                                AgentActionExecutionResult.tool(observation),
                                                AgentActionExecutionResult.reason("基于法律依据完成分析"));

                when(reflectionService.reflect(any(), any()))
                                .thenReturn(
                                                ReflectionResult.of(ReflectionDecision.CONTINUE, "继续"),
                                                ReflectionResult.of(ReflectionDecision.CONTINUE, "完成"));

                when(finalAnswerService.generate(any()))
                                .thenReturn("劳动合同解除应根据不同解除类型分别满足法定条件。");

                RecordingPublisher publisher = new RecordingPublisher();

                AgentContext result = runtime.run(
                                AgentContext.from("检索劳动合同解除需要满足哪些法律条件"),
                                publisher);

                assertEquals(AgentStatus.FINISHED, result.getStatus());

                assertEquals(
                                List.of(
                                                AgentStreamEventType.AGENT_STARTED,
                                                AgentStreamEventType.PLANNING_STARTED,
                                                AgentStreamEventType.PLANNING_COMPLETED,
                                                AgentStreamEventType.TASK_STARTED,
                                                AgentStreamEventType.TOOL_STARTED,
                                                AgentStreamEventType.TOOL_COMPLETED,
                                                AgentStreamEventType.REFLECTION_STARTED,
                                                AgentStreamEventType.REFLECTION_COMPLETED,
                                                AgentStreamEventType.TASK_STARTED,
                                                AgentStreamEventType.REASONING_STARTED,
                                                AgentStreamEventType.REASONING_COMPLETED,
                                                AgentStreamEventType.REFLECTION_STARTED,
                                                AgentStreamEventType.REFLECTION_COMPLETED,
                                                AgentStreamEventType.FINAL_ANSWER_STARTED),
                                publisher.types());
        }

        @Test
        void shouldPublishAgentFailedWhenRuntimeThrows() {
                when(agentPipeline.execute(any()))
                                .thenThrow(new IllegalStateException("pipeline failed"));

                RecordingPublisher publisher = new RecordingPublisher();

                assertThrows(
                                IllegalStateException.class,
                                () -> runtime.run(
                                                AgentContext.from("分析劳动合同"),
                                                publisher));

                assertEquals(
                                List.of(
                                                AgentStreamEventType.AGENT_STARTED,
                                                AgentStreamEventType.PLANNING_STARTED,
                                                AgentStreamEventType.AGENT_FAILED),
                                publisher.types());
        }

        @Test
        void shouldKeepSynchronousRunCompatible() {
                AgentContext initialized = AgentContext.builder()
                                .goal("分析劳动合同")
                                .agentPlan(AgentPlan.from(List.of()))
                                .status(AgentStatus.RUNNING)
                                .build();

                when(agentPipeline.execute(any()))
                                .thenReturn(initialized);

                when(finalAnswerService.generate(any()))
                                .thenReturn("同步执行结果");

                AgentContext result = runtime.run(
                                AgentContext.from("分析劳动合同"));

                assertEquals(AgentStatus.FINISHED, result.getStatus());
                assertEquals("同步执行结果", result.getFinalAnswer());
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
        }
}
