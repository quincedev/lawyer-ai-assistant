package com.quince.lawyeraiassistant.security.legal.evidence;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.quince.lawyeraiassistant.agent.model.ToolObservation;
import com.quince.lawyeraiassistant.security.legal.LegalSecurityContext;
import com.quince.lawyeraiassistant.security.legal.SecuritySource;
import com.quince.lawyeraiassistant.security.legal.SecurityTrustLevel;

@Component
public final class LegalEvidenceTrustPolicy {

    public LegalSecurityContext validate(
            ToolObservation observation) {

        Objects.requireNonNull(
                observation,
                "ToolObservation must not be null");

        LegalSecurityContext securityContext = observation.getEvidenceSecurityContext()
                .orElseThrow(
                        () -> new IllegalStateException(
                                "ToolObservation security context is missing"));

        SecuritySource source = securityContext.source();

        SecurityTrustLevel trustLevel = securityContext.trustLevel();

        if (source == SecuritySource.TOOL_RESULT
                || source == SecuritySource.MCP_RESULT) {

            if (trustLevel != SecurityTrustLevel.UNTRUSTED) {

                throw new IllegalStateException(
                        "External evidence must remain UNTRUSTED");
            }

            return securityContext;
        }

        if (source == SecuritySource.RUNTIME) {

            if (trustLevel != SecurityTrustLevel.DERIVED) {

                throw new IllegalStateException(
                        "Runtime observation must remain DERIVED");
            }

            return securityContext;
        }

        throw new IllegalStateException(
                "Unsupported ToolObservation security source: "
                        + source);
    }
}