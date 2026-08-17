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

/**
 * Default application service for Agent execution.
 *
 * <p>
 * This service represents the application boundary between
 * external adapters and the Agent Runtime.
 * </p>
 *
 * <pre>
 * External Input
 *      ↓
 * InputGuardrailChain
 *      ↓
 * ALLOW / BLOCK
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

        public DefaultAgentApplicationService(
                        AgentRuntime agentRuntime,
                        InputGuardrailChain inputGuardrailChain,
                        OutputGuardrailChain outputGuardrailChain,
                        SecurityAuditLogger securityAuditLogger) {

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
        }

        @Override
        public AgentContext execute(
                        String goal) {

                GuardrailResult inputResult = inputGuardrailChain.evaluate(
                                goal);

                enforceInputGuardrail(
                                inputResult);

                AgentContext initialContext = AgentContext.from(
                                goal);

                AgentContext result = agentRuntime.run(
                                initialContext);

                GuardrailResult outputResult = outputGuardrailChain.evaluate(
                                result.getFinalAnswer());

                enforceOutputGuardrail(
                                outputResult);

                return result;
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
}