package com.quince.lawyeraiassistant.agent.application;

import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.runtime.AgentRuntime;
import com.quince.lawyeraiassistant.security.audit.SecurityAuditEvent;
import com.quince.lawyeraiassistant.security.audit.SecurityAuditEventType;
import com.quince.lawyeraiassistant.security.audit.SecurityAuditLogger;
import com.quince.lawyeraiassistant.security.guardrail.GuardrailResult;
import com.quince.lawyeraiassistant.security.guardrail.exception.InputGuardrailViolationException;
import com.quince.lawyeraiassistant.security.guardrail.exception.OutputGuardrailViolationException;
import com.quince.lawyeraiassistant.security.guardrail.input.InputGuardrailChain;
import com.quince.lawyeraiassistant.security.guardrail.output.OutputGuardrailChain;
import com.quince.lawyeraiassistant.security.tenant.TenantContext;
import com.quince.lawyeraiassistant.security.tenant.TenantContextProvider;
import com.quince.lawyeraiassistant.security.tenant.authorization.TenantAccessDeniedException;
import com.quince.lawyeraiassistant.security.tenant.authorization.TenantAuthorizationService;
import com.quince.lawyeraiassistant.security.tenant.quota.TenantQuotaLease;
import com.quince.lawyeraiassistant.security.tenant.quota.TenantResourceQuotaExceededException;
import com.quince.lawyeraiassistant.security.tenant.quota.TenantResourceQuotaService;

/**
 * Default application service for Agent execution.
 *
 * <pre>
 * External Input
 *      ↓
 * InputGuardrailChain
 *      ↓
 * TenantContext
 *      ↓
 * Tenant Authorization
 *      ↓
 * AgentContext
 *      ↓
 * AgentRuntime
 *      ↓
 * OutputGuardrailChain
 * </pre>
 */
@Service
public class DefaultAgentApplicationService
                implements AgentApplicationService {

        private final InputGuardrailChain inputGuardrailChain;

        private final AgentRuntime agentRuntime;

        private final OutputGuardrailChain outputGuardrailChain;

        private final SecurityAuditLogger securityAuditLogger;

        private final TenantContextProvider tenantContextProvider;

        private final TenantAuthorizationService tenantAuthorizationService;

        private final TenantResourceQuotaService tenantResourceQuotaService;

        public DefaultAgentApplicationService(
                        AgentRuntime agentRuntime,
                        InputGuardrailChain inputGuardrailChain,
                        OutputGuardrailChain outputGuardrailChain,
                        SecurityAuditLogger securityAuditLogger,
                        TenantContextProvider tenantContextProvider,
                        TenantAuthorizationService tenantAuthorizationService,
                        TenantResourceQuotaService tenantResourceQuotaService) {

                this.agentRuntime = Objects.requireNonNull(
                                agentRuntime,
                                "agentRuntime must not be null");

                this.inputGuardrailChain = Objects.requireNonNull(
                                inputGuardrailChain,
                                "inputGuardrailChain must not be null");

                this.outputGuardrailChain = Objects.requireNonNull(
                                outputGuardrailChain,
                                "outputGuardrailChain must not be null");

                this.securityAuditLogger = Objects.requireNonNull(
                                securityAuditLogger,
                                "securityAuditLogger must not be null");

                this.tenantContextProvider = Objects.requireNonNull(
                                tenantContextProvider,
                                "tenantContextProvider must not be null");

                this.tenantAuthorizationService = Objects.requireNonNull(
                                tenantAuthorizationService,
                                "tenantAuthorizationService must not be null");

                this.tenantResourceQuotaService = Objects.requireNonNull(
                                tenantResourceQuotaService,
                                "tenantResourceQuotaService must not be null");
        }

        @Override
        public AgentContext execute(
                        String goal) {

                GuardrailResult inputResult = inputGuardrailChain.evaluate(
                                goal);

                enforceInputGuardrail(
                                inputResult);

                TenantContext tenantContext = tenantContextProvider.current();

                authorizeTenant(
                                tenantContext);

                AgentContext initialContext = AgentContext.authenticated(
                                goal,
                                tenantContext);

                AgentContext result;

                try (TenantQuotaLease ignored = acquireTenantExecutionQuota(
                                tenantContext)) {

                        result = agentRuntime.run(
                                        initialContext);
                }

                GuardrailResult outputResult = outputGuardrailChain.evaluate(
                                result.getFinalAnswer());

                enforceOutputGuardrail(
                                outputResult);

                return result;
        }

        private void authorizeTenant(
                        TenantContext tenantContext) {

                try {

                        tenantAuthorizationService.authorizeAgentAccess(
                                        tenantContext);

                } catch (TenantAccessDeniedException exception) {

                        securityAuditLogger.log(
                                        SecurityAuditEvent.warn(
                                                        SecurityAuditEventType.TENANT_ACCESS_DENIED,
                                                        "DefaultAgentApplicationService",
                                                        "Tenant access denied",
                                                        Map.of(
                                                                        "tenantId",
                                                                        tenantContext.tenantId(),
                                                                        "userId",
                                                                        tenantContext.userId())));

                        throw exception;
                }
        }

        private void enforceInputGuardrail(
                        GuardrailResult result) {

                Objects.requireNonNull(
                                result,
                                "guardrailResult must not be null");

                if (!result.isBlocked()) {
                        return;
                }

                securityAuditLogger.log(
                                SecurityAuditEvent.warn(
                                                SecurityAuditEventType.INPUT_GUARDRAIL_BLOCKED,
                                                "DefaultAgentApplicationService",
                                                result.reason(),
                                                Map.of(
                                                                "guardrail",
                                                                result.guardrailName())));

                throw new InputGuardrailViolationException(
                                result.guardrailName());
        }

        private void enforceOutputGuardrail(
                        GuardrailResult result) {

                Objects.requireNonNull(
                                result,
                                "guardrailResult must not be null");

                if (!result.isBlocked()) {
                        return;
                }

                securityAuditLogger.log(
                                SecurityAuditEvent.warn(
                                                SecurityAuditEventType.OUTPUT_GUARDRAIL_BLOCKED,
                                                "DefaultAgentApplicationService",
                                                result.reason(),
                                                Map.of(
                                                                "guardrail",
                                                                result.guardrailName())));

                throw new OutputGuardrailViolationException();
        }

        private TenantQuotaLease acquireTenantExecutionQuota(
                        TenantContext tenantContext) {

                try {

                        return tenantResourceQuotaService
                                        .acquireAgentExecution(
                                                        tenantContext);

                } catch (TenantResourceQuotaExceededException exception) {

                        securityAuditLogger.log(
                                        SecurityAuditEvent.warn(
                                                        SecurityAuditEventType.TENANT_RESOURCE_QUOTA_EXCEEDED,
                                                        "DefaultAgentApplicationService",
                                                        "Tenant Agent execution quota exceeded",
                                                        Map.of(
                                                                        "tenantId",
                                                                        tenantContext.tenantId(),
                                                                        "userId",
                                                                        tenantContext.userId())));

                        throw exception;
                }
        }
}