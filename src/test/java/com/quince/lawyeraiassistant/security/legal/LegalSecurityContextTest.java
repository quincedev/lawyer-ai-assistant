package com.quince.lawyeraiassistant.security.legal;

import com.quince.lawyeraiassistant.security.SecurityTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SecurityTest
class LegalSecurityContextTest {

    @Test
    void shouldCreateUntrustedContext() {

        LegalSecurityContext context = LegalSecurityContext.of(
                SecuritySource.MCP_RESULT,
                SecurityTrustLevel.UNTRUSTED);

        assertTrue(
                context.isUntrusted());

        assertFalse(
                context.isTrusted());

        assertTrue(
                context.signals()
                        .isEmpty());
    }

    @Test
    void shouldAppendSignalImmutably() {

        LegalSecurityContext original = LegalSecurityContext.of(
                SecuritySource.MCP_RESULT,
                SecurityTrustLevel.UNTRUSTED);

        SecuritySignal signal = SecuritySignal.of(
                SecuritySignalType.INDIRECT_PROMPT_INJECTION,
                SecuritySource.MCP_RESULT,
                "suspicious instruction");

        LegalSecurityContext updated = original.withSignal(
                signal);

        assertTrue(
                original.signals()
                        .isEmpty());

        assertEquals(
                1,
                updated.signals()
                        .size());

        assertTrue(
                updated.hasSignal(
                        SecuritySignalType.INDIRECT_PROMPT_INJECTION));
    }
}
