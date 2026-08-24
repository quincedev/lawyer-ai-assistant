package com.quince.lawyeraiassistant.agent.runtime.evidence;

import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.quince.lawyeraiassistant.agent.model.AgentTask;
import com.quince.lawyeraiassistant.agent.model.ToolObservation;
import com.quince.lawyeraiassistant.security.audit.SecurityAuditEvent;
import com.quince.lawyeraiassistant.security.audit.SecurityAuditEventType;
import com.quince.lawyeraiassistant.security.audit.SecurityAuditLogger;
import com.quince.lawyeraiassistant.security.legal.evidence.LegalEvidenceTrustPolicy;

@Component
public final class DefaultAgentEvidencePolicy
                implements AgentEvidencePolicy {

        private static final String AUDIT_SOURCE = "DefaultAgentRuntime";

        private static final String REJECTION_REASON = "Evidence trust boundary rejected tool result";

        private final LegalEvidenceTrustPolicy legalEvidenceTrustPolicy;

        private final SecurityAuditLogger securityAuditLogger;

        public DefaultAgentEvidencePolicy(
                        LegalEvidenceTrustPolicy legalEvidenceTrustPolicy,
                        SecurityAuditLogger securityAuditLogger) {

                this.legalEvidenceTrustPolicy = Objects.requireNonNull(
                                legalEvidenceTrustPolicy,
                                "legalEvidenceTrustPolicy must not be null");

                this.securityAuditLogger = Objects.requireNonNull(
                                securityAuditLogger,
                                "securityAuditLogger must not be null");
        }

        @Override
        public EvidenceValidationResult validate(
                        AgentTask task,
                        ToolObservation observation) {

                Objects.requireNonNull(
                                task,
                                "AgentTask must not be null");

                try {

                        legalEvidenceTrustPolicy.validate(
                                        observation);

                        return EvidenceValidationResult.allow();

                } catch (RuntimeException exception) {

                        securityAuditLogger.log(
                                        SecurityAuditEvent.warn(
                                                        SecurityAuditEventType.EVIDENCE_TRUST_REJECTED,
                                                        AUDIT_SOURCE,
                                                        REJECTION_REASON,
                                                        Map.of(
                                                                        "taskId",
                                                                        task.getId(),
                                                                        "toolName",
                                                                        observation == null
                                                                                        ? "unknown"
                                                                                        : observation.getToolName())));

                        return EvidenceValidationResult.reject(
                                        REJECTION_REASON);
                }
        }
}