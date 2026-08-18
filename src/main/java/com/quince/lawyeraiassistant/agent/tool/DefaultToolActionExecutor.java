package com.quince.lawyeraiassistant.agent.tool;

import com.quince.lawyeraiassistant.agent.model.ToolAction;
import com.quince.lawyeraiassistant.agent.model.ToolExecutionResult;
import com.quince.lawyeraiassistant.agent.model.ToolObservation;
import com.quince.lawyeraiassistant.security.audit.SecurityAuditEvent;
import com.quince.lawyeraiassistant.security.audit.SecurityAuditEventType;
import com.quince.lawyeraiassistant.security.audit.SecurityAuditLogger;
import com.quince.lawyeraiassistant.security.legal.LegalSecurityContext;
import com.quince.lawyeraiassistant.security.legal.SecuritySource;
import com.quince.lawyeraiassistant.security.legal.SecurityTrustLevel;
import com.quince.lawyeraiassistant.security.runtime.AgentExecutionLimits;

import jakarta.annotation.PreDestroy;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * ToolActionExecutor 默认实现。
 */
@Component
public class DefaultToolActionExecutor
                implements ToolActionExecutor {

        private final AgentToolRegistry toolRegistry;

        private final AgentExecutionLimits executionLimits;

        private final ExecutorService executorService;

        private final SecurityAuditLogger securityAuditLogger;

        public DefaultToolActionExecutor(
                        AgentToolRegistry toolRegistry,
                        AgentExecutionLimits executionLimits,
                        SecurityAuditLogger securityAuditLogger) {

                this.toolRegistry = Objects.requireNonNull(
                                toolRegistry,
                                "toolRegistry must not be null");

                this.executionLimits = Objects.requireNonNull(
                                executionLimits,
                                "executionLimits must not be null");

                this.securityAuditLogger = Objects.requireNonNull(
                                securityAuditLogger,
                                "securityAuditLogger must not be null");

                this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        }

        @Override
        public ToolObservation execute(
                        ToolAction action) {

                return execute(
                                ToolExecutionContext.sharedOnly(),
                                action);
        }

        @Override
        public ToolObservation execute(
                        ToolExecutionContext executionContext,
                        ToolAction action) {

                Objects.requireNonNull(
                                executionContext,
                                "ToolExecutionContext must not be null");

                Objects.requireNonNull(
                                action,
                                "ToolAction must not be null");

                AgentTool tool = toolRegistry.get(
                                action.getToolName());

                SecuritySource resultSource = Objects.requireNonNull(
                                tool.resultSecuritySource(),
                                "Tool result security source must not be null");

                LegalSecurityContext evidenceSecurityContext = LegalSecurityContext.of(
                                resultSource,
                                SecurityTrustLevel.UNTRUSTED);

                Future<ToolExecutionResult> future = executorService.submit(
                                () -> tool.execute(
                                                executionContext,
                                                action));

                ToolExecutionResult result;

                try {

                        result = future.get(
                                        executionLimits
                                                        .maxToolExecutionTime()
                                                        .toMillis(),
                                        TimeUnit.MILLISECONDS);

                } catch (TimeoutException exception) {

                        future.cancel(true);

                        securityAuditLogger.log(
                                        SecurityAuditEvent.warn(
                                                        SecurityAuditEventType.TOOL_EXECUTION_TIMEOUT,
                                                        "DefaultToolActionExecutor",
                                                        "Tool execution timed out",
                                                        Map.of(
                                                                        "toolName",
                                                                        action.getToolName(),
                                                                        "taskId",
                                                                        action.getTaskId(),
                                                                        "timeoutMs",
                                                                        String.valueOf(
                                                                                        executionLimits
                                                                                                        .maxToolExecutionTime()
                                                                                                        .toMillis()))));

                        return ToolObservation.failure(
                                        action.getTaskId(),
                                        action.getToolName(),
                                        "Tool execution timed out",
                                        evidenceSecurityContext);

                } catch (InterruptedException exception) {

                        future.cancel(true);

                        Thread.currentThread()
                                        .interrupt();

                        return ToolObservation.failure(
                                        action.getTaskId(),
                                        action.getToolName(),
                                        "Tool execution interrupted",
                                        evidenceSecurityContext);

                } catch (ExecutionException exception) {

                        Throwable cause = exception.getCause();

                        String message = cause == null
                                        || cause.getMessage() == null
                                        || cause.getMessage().isBlank()
                                                        ? "Tool execution failed"
                                                        : cause.getMessage();

                        return ToolObservation.failure(
                                        action.getTaskId(),
                                        action.getToolName(),
                                        message,
                                        evidenceSecurityContext);
                }

                Objects.requireNonNull(
                                result,
                                "ToolExecutionResult must not be null");

                if (result.isSuccess()) {

                        return ToolObservation.success(
                                        action.getTaskId(),
                                        action.getToolName(),
                                        result.getContent(),
                                        evidenceSecurityContext);
                }

                return ToolObservation.failure(
                                action.getTaskId(),
                                action.getToolName(),
                                result.getErrorMessage(),
                                evidenceSecurityContext);
        }

        @PreDestroy
        void shutdown() {

                executorService.close();
        }
}