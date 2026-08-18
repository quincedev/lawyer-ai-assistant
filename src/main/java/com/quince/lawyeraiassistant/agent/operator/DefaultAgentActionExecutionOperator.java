package com.quince.lawyeraiassistant.agent.operator;

import com.quince.lawyeraiassistant.agent.model.AgentAction;
import com.quince.lawyeraiassistant.agent.model.AgentActionExecutionResult;
import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentTask;
import com.quince.lawyeraiassistant.agent.model.ToolAction;
import com.quince.lawyeraiassistant.agent.model.ToolObservation;
import com.quince.lawyeraiassistant.agent.service.AgentFinalAnswerService;
import com.quince.lawyeraiassistant.agent.service.AgentRuntimeReasonService;
import com.quince.lawyeraiassistant.agent.tool.ToolActionExecutor;
import com.quince.lawyeraiassistant.agent.tool.ToolExecutionContext;
import com.quince.lawyeraiassistant.security.audit.SecurityAuditEvent;
import com.quince.lawyeraiassistant.security.audit.SecurityAuditEventType;
import com.quince.lawyeraiassistant.security.audit.SecurityAuditLogger;
import com.quince.lawyeraiassistant.security.authorization.tool.ToolAuthorizationResult;
import com.quince.lawyeraiassistant.security.authorization.tool.ToolAuthorizationService;
import com.quince.lawyeraiassistant.security.legal.LegalSecurityContext;
import com.quince.lawyeraiassistant.security.legal.SecuritySource;
import com.quince.lawyeraiassistant.security.legal.SecurityTrustLevel;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * AgentActionExecutionOperator 默认实现。
 *
 * <p>
 * 根据 AgentActionType 将 Action 分派到对应执行能力。
 * </p>
 */
@Component
public class DefaultAgentActionExecutionOperator
                implements AgentActionExecutionOperator {

        private final ToolActionExecutor toolActionExecutor;

        private final AgentRuntimeReasonService runtimeReasonService;

        private final AgentFinalAnswerService finalAnswerService;

        private final ToolAuthorizationService toolAuthorizationService;

        private final SecurityAuditLogger securityAuditLogger;

        public DefaultAgentActionExecutionOperator(
                        ToolActionExecutor toolActionExecutor,
                        AgentRuntimeReasonService runtimeReasonService,
                        AgentFinalAnswerService finalAnswerService,
                        ToolAuthorizationService toolAuthorizationService,
                        SecurityAuditLogger securityAuditLogger) {

                this.toolActionExecutor = Objects.requireNonNull(
                                toolActionExecutor,
                                "ToolActionExecutor must not be null");

                this.runtimeReasonService = Objects.requireNonNull(
                                runtimeReasonService,
                                "AgentRuntimeReasonService must not be null");

                this.finalAnswerService = Objects.requireNonNull(
                                finalAnswerService,
                                "AgentFinalAnswerService must not be null");

                this.toolAuthorizationService = Objects.requireNonNull(
                                toolAuthorizationService,
                                "ToolAuthorizationService must not be null");

                this.securityAuditLogger = Objects.requireNonNull(
                                securityAuditLogger,
                                "securityAuditLogger must not be null");
        }

        @Override
        public AgentActionExecutionResult execute(
                        AgentContext context,
                        AgentTask task,
                        AgentAction action) {

                Objects.requireNonNull(
                                context,
                                "AgentContext must not be null");

                Objects.requireNonNull(
                                task,
                                "AgentTask must not be null");

                Objects.requireNonNull(
                                action,
                                "AgentAction must not be null");

                validateTaskMatch(
                                task,
                                action);

                return switch (action.getType()) {

                        case TOOL ->
                                executeTool(
                                                context,
                                                action);

                        case REASON ->
                                executeReason(
                                                context,
                                                task);

                        case FINAL_ANSWER ->
                                executeFinalAnswer(
                                                context);
                };
        }

        private AgentActionExecutionResult executeTool(
                        AgentContext context,
                        AgentAction action) {

                ToolAction toolAction = action.requireToolAction();

                ToolAuthorizationResult authorization = toolAuthorizationService.authorize(
                                context,
                                toolAction);

                if (authorization.isDenied()) {
                        LegalSecurityContext securityContext = context.getLegalSecurityContext()
                                        .orElse(null);

                        Map<String, String> metadata = new HashMap<>();

                        metadata.put(
                                        "toolName",
                                        toolAction.getToolName());

                        metadata.put(
                                        "taskId",
                                        toolAction.getTaskId());

                        metadata.put(
                                        "policyName",
                                        authorization.policyName());

                        if (securityContext != null) {

                                metadata.put(
                                                "source",
                                                securityContext.source().name());

                                metadata.put(
                                                "trustLevel",
                                                securityContext.trustLevel().name());
                        }

                        securityAuditLogger.log(
                                        SecurityAuditEvent.warn(
                                                        SecurityAuditEventType.TOOL_AUTHORIZATION_DENIED,
                                                        "DefaultAgentActionExecutionOperator",
                                                        authorization.reason(),
                                                        metadata));

                        ToolObservation deniedObservation = ToolObservation.failure(
                                        toolAction.getTaskId(),
                                        toolAction.getToolName(),
                                        authorization.reason(),
                                        LegalSecurityContext.of(
                                                        SecuritySource.RUNTIME,
                                                        SecurityTrustLevel.DERIVED));

                        return AgentActionExecutionResult.tool(
                                        deniedObservation);
                }

                ToolExecutionContext executionContext = ToolExecutionContext.from(
                                context);

                ToolObservation observation = toolActionExecutor.execute(
                                executionContext,
                                toolAction);

                return AgentActionExecutionResult.tool(
                                observation);
        }

        private AgentActionExecutionResult executeReason(
                        AgentContext context,
                        AgentTask task) {

                String reason = runtimeReasonService.reason(
                                context,
                                task);

                return AgentActionExecutionResult.reason(
                                reason);
        }

        private AgentActionExecutionResult executeFinalAnswer(
                        AgentContext context) {

                String finalAnswer = finalAnswerService.generate(
                                context);

                return AgentActionExecutionResult.finalAnswer(
                                finalAnswer);
        }

        private void validateTaskMatch(
                        AgentTask task,
                        AgentAction action) {

                if (!task.getId()
                                .equals(
                                                action.getTaskId())) {

                        throw new IllegalArgumentException(
                                        "AgentAction taskId must match AgentTask id");
                }
        }
}