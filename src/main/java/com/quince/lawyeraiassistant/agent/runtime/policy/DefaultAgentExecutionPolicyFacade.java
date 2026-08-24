package com.quince.lawyeraiassistant.agent.runtime.policy;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;

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

@Component
public final class DefaultAgentExecutionPolicyFacade
        implements AgentExecutionPolicyFacade {

    private static final String AUDIT_SOURCE = "DefaultAgentRuntime";

    private final AgentExecutionLimits executionLimits;

    private final RuntimeGuardrailService runtimeGuardrailService;

    private final RuntimeResourceGuardrailService runtimeResourceGuardrailService;

    private final SecurityAuditLogger securityAuditLogger;

    public DefaultAgentExecutionPolicyFacade(
            AgentExecutionLimits executionLimits,
            RuntimeGuardrailService runtimeGuardrailService,
            RuntimeResourceGuardrailService runtimeResourceGuardrailService,
            SecurityAuditLogger securityAuditLogger) {

        this.executionLimits = Objects.requireNonNull(
                executionLimits,
                "executionLimits must not be null");

        this.runtimeGuardrailService = Objects.requireNonNull(
                runtimeGuardrailService,
                "runtimeGuardrailService must not be null");

        this.runtimeResourceGuardrailService = Objects.requireNonNull(
                runtimeResourceGuardrailService,
                "runtimeResourceGuardrailService must not be null");

        this.securityAuditLogger = Objects.requireNonNull(
                securityAuditLogger,
                "securityAuditLogger must not be null");
    }

    @Override
    public AgentExecutionBudget createBudget() {

        return new AgentExecutionBudget(
                executionLimits);
    }

    @Override
    public RuntimeGuardrailResult evaluateRuntime(
            RuntimeGuardrailOperation operation,
            AgentExecutionBudget budget) {

        Objects.requireNonNull(
                operation,
                "RuntimeGuardrailOperation must not be null");

        Objects.requireNonNull(
                budget,
                "AgentExecutionBudget must not be null");

        RuntimeGuardrailResult result = runtimeGuardrailService.evaluate(
                operation,
                budget);

        Objects.requireNonNull(
                result,
                "RuntimeGuardrailResult must not be null");

        if (result.isDenied()) {

            auditRuntimeLimit(
                    operation,
                    result);
        }

        return result;
    }

    @Override
    public RuntimeResourceResult evaluateResource(
            RuntimeResourceType resourceType,
            int actualLength,
            String taskId) {

        Objects.requireNonNull(
                resourceType,
                "RuntimeResourceType must not be null");

        RuntimeResourceResult result = runtimeResourceGuardrailService.evaluate(
                resourceType,
                actualLength);

        Objects.requireNonNull(
                result,
                "RuntimeResourceResult must not be null");

        if (result.isDenied()) {

            auditResourceLimit(
                    resourceType,
                    actualLength,
                    result,
                    taskId);
        }

        return result;
    }

    private void auditRuntimeLimit(
            RuntimeGuardrailOperation operation,
            RuntimeGuardrailResult result) {

        securityAuditLogger.log(
                SecurityAuditEvent.warn(
                        SecurityAuditEventType.RUNTIME_LIMIT_REACHED,
                        AUDIT_SOURCE,
                        result.reason(),
                        Map.of(
                                "operation",
                                operation.name(),
                                "policyName",
                                result.policyName())));
    }

    private void auditResourceLimit(
            RuntimeResourceType resourceType,
            int actualLength,
            RuntimeResourceResult result,
            String taskId) {

        Map<String, String> metadata = new HashMap<>();

        metadata.put(
                "resourceType",
                resourceType.name());

        metadata.put(
                "actualLength",
                String.valueOf(
                        actualLength));

        metadata.put(
                "policyName",
                result.policyName());

        if (taskId != null
                && !taskId.isBlank()) {

            metadata.put(
                    "taskId",
                    taskId);
        }

        securityAuditLogger.log(
                SecurityAuditEvent.warn(
                        SecurityAuditEventType.RESOURCE_LIMIT_REACHED,
                        AUDIT_SOURCE,
                        result.reason(),
                        metadata));
    }
}